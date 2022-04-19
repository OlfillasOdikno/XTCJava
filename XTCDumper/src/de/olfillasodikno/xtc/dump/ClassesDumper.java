package de.olfillasodikno.xtc.dump;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import com.github.jonatino.misc.MemoryBuffer;
import com.github.jonatino.process.Module;
import com.github.jonatino.process.Process;
import com.github.jonatino.process.Processes;

import de.olfillasodikno.xtc.dump.ClassesDumper.SendProp.SendPropType;

public class ClassesDumper {

	private static final File DEFAULT_ICE_KEY_FILE = new File("ice_key");
	private static final File DEFAULT_SERVER_DUMP_FILE = new File("server_dump");

	private static Process process;
	private static Module server;
	private static Module engine;

	private Map<Integer, ServerClassInfo> serverClasses = new HashMap<>();
	private static Map<String, SendTable> sendTables = new HashMap<>();
	private static Map<String, RecvTable> recvTables = new HashMap<>();

	private Map<String, ArrayList<SendProp>> flatTables = new HashMap<>();

	private byte[] iceKey;
	
	public byte[] getIceKey() {
		return iceKey;
	}
	
	public Map<String, ArrayList<SendProp>> getFlatTables() {
		return flatTables;
	}

	public Map<Integer, ServerClassInfo> getServerClasses() {
		return serverClasses;
	}

	public static void main(String[] args) {
		new ClassesDumper();
	}

	public ClassesDumper() {
		waitUntilFound("process", () -> (process = Processes.byName("csgo.exe")) != null);
		waitUntilFound("server module", () -> (server = process.findModule("server.dll")) != null);
		waitUntilFound("engine module", () -> (engine = process.findModule("engine.dll")) != null);
		dumpICEKey();
		dumpServer();
	}


	private void dumpICEKey() {
		byte[] key = new byte[16];
		boolean[] mask = PatternScanner.convMask("xx????xx????x");
		byte[] signature = PatternScanner
				.convSignature("\\x8B\\x82\\x00\\x00\\x00\\x00\\xC3\\xB8\\x00\\x00\\x00\\x00\\xC3");
		long start = PatternScanner.fromPattern(engine, signature, mask, 8, true);
		if (start == -1) {
			System.out.println("Key not found");
			return;
		}
		for (int i = 0; i < key.length; i++) {
			key[i] = (byte) process.readByte(start + i);
		}
		try {
			Files.write(DEFAULT_ICE_KEY_FILE.toPath(), key,StandardOpenOption.WRITE, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
		} catch (IOException e) {
			e.printStackTrace();
		}
		System.out.println("Finished ICE_KEY Dump");
	}

	private void dumpServer() {
		long start = getHeadAddr();
		long base = Integer.toUnsignedLong(server.data().getInt(start - server.address()));
		ServerClass head = new ServerClass().fromAddr(base);

		buildServerClasses(head);

		boolean[] mask = PatternScanner.convMask("xxxxx????xx????");
		byte[] signature = PatternScanner
				.convSignature("\\x66\\x3B\\xC6\\x0F\\x84\\x00\\x00\\x00\\x00\\x8B\\x0D\\x00\\x00\\x00\\x00");
		start = PatternScanner.fromPattern(engine, signature, mask, 11, true);
		base = Integer.toUnsignedLong(process.readInt(start));
		int size = 8;
		int i = 0;
		ArrayList<LinkedListEntry> entries = new ArrayList<>();
		while (true) {
			long addr = base + i * size;
			LinkedListEntry entry = new LinkedListEntry(addr);
			entries.add(entry);
			if (entry.getB() == (short) 0xFFFF) {
				break;
			}
			i++;
		}
		entries.stream().forEach(e -> {
			CRecvDecoder decoder = new CRecvDecoder().fromAddr(e.getPtr());
			ArrayList<SendProp> props = new ArrayList<>();
			decoder.getPropsVec().forEach(p -> {
				SendProp sp = new SendProp().fromAddr(p.getPtr());
				props.add(sp);
			});
			flatTables.put(decoder.getRecvTable().getTableName().toLowerCase(), props);
		});

		ByteBuffer buf = ByteBuffer.allocate(1024 * 1024 * 16); // 16mb max should be enough
		writeServer(buf);
		int length = buf.position();

		try (FileOutputStream fos = new FileOutputStream(DEFAULT_SERVER_DUMP_FILE)) {
			fos.write(buf.array(), 0, length);
		} catch (IOException e) {
			e.printStackTrace();
		}
		System.out.println("Finished Server Dump");
	}

	private void writeServer(ByteBuffer buf) {
		writeServerClasses(buf);
		writeFlatTables(buf);
	}

	private void writeFlatTables(ByteBuffer buf) {
		buf.putInt(flatTables.size());
		for (Map.Entry<String, ArrayList<SendProp>> entry : flatTables.entrySet()) {
			writeString(buf, entry.getKey());
			writeSendProps(buf, entry.getValue());
		}
	}

	private void writeSendProps(ByteBuffer buf, ArrayList<SendProp> props) {
		buf.putInt(props.size());
		for (int i = 0; i < props.size(); i++) {
			writeSendProp(buf, props.get(i));
		}
	}

	private void writeSendProp(ByteBuffer buf, SendProp sendProp) {

		buf.putInt(sendProp.type.ordinal());
		buf.putInt(sendProp.nBits);
		buf.putFloat(sendProp.fLowValue);
		buf.putFloat(sendProp.fHighValue);
		buf.putInt(sendProp.nElements);
		buf.putInt(sendProp.ElementStride);
		buf.putInt(sendProp.flags);
		if (sendProp.type == SendPropType.DPT_Array) {
			writeSendProp(buf, sendProp.template);
		}
		writeString(buf, sendProp.varName);
		if ((sendProp.flags & (1 << 6)) != 0) {
			writeString(buf, sendProp.excludeDTName);
		}

		if (sendProp.type == SendPropType.DPT_DataTable) {
			writeDataTable(buf, sendProp.subTable);
		}
	}

	private void writeDataTable(ByteBuffer buf, SendTable tb) {
		buf.putInt(tb.nProps);
		for (int i = 0; i < tb.nProps; i++) {
			SendProp prop = tb.props[i];
			writeSendProp(buf, prop);
		}
		writeString(buf, tb.tableName);
	}

	private void writeServerClasses(ByteBuffer buf) {
		buf.putInt(serverClasses.size());
		for (Map.Entry<Integer, ServerClassInfo> entry : serverClasses.entrySet()) {
			buf.putInt(entry.getKey());
			writeServerClassInfo(buf, entry.getValue());
		}
	}

	private void writeServerClassInfo(ByteBuffer buf, ServerClassInfo info) {
		writeString(buf, info.className);
		writeString(buf, info.datatableName);
		buf.putInt(info.instanceBaselineIndex);
	}

	private void writeString(ByteBuffer buf, String s) {
		buf.putInt(s.length());
		buf.put(s.getBytes());
	}

	private void buildServerClasses(ServerClass head) {
		int id = 0;
		for (; head.next != null; head = head.next) {
			head.classID = id++;
			ServerClassInfo info = new ServerClassInfo();
			info.className = head.networkName;
			info.datatableName = head.sendTable.tableName;
			serverClasses.put(head.classID, info);
		}
	}

	private static long getHeadAddr() {
		boolean[] mask = PatternScanner.convMask("xxxxxx????xxxxxxx");
		byte[] signature = PatternScanner
				.convSignature("\\x55\\x8B\\xEC\\x51\\x8B\\x0D\\x00\\x00\\x00\\x00\\x89\\x4D\\xFC\\x85\\xC9\\x75\\x14");
		long start = PatternScanner.fromPattern(server, signature, mask, 6, true);
		return start;
	}

	private static void waitUntilFound(String message, Clause clause) {
		System.out.print("Looking for " + message + ". Please wait.");
		while (!clause.get())
			try {
				Thread.sleep(3000);
				System.out.print(".");
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		System.out.println("\nFound " + message + "!");
	}

	@FunctionalInterface
	private interface Clause {
		boolean get();
	}

	public static class ServerClassInfo {
		private String className;
		private String datatableName;
		private int instanceBaselineIndex;

		public String getClassName() {
			return className;
		}

		public String getDatatableName() {
			return datatableName;
		}

		public int getInstanceBaselineIndex() {
			return instanceBaselineIndex;
		}
	}

	public static class CRecvDecoder {

		private ArrayList<VectorEntry> propsVec;

		private RecvTable recvTable;

		public CRecvDecoder fromAddr(long addr) {
			MemoryBuffer buf = process.read(addr, 0x48);
			long rcvTablePtr = Integer.toUnsignedLong(buf.getInt(0));
			long clientSendTablePtr = Integer.toUnsignedLong(buf.getInt(4));

			long propsVecPtr = Integer.toUnsignedLong(buf.getInt(0x34));
			int propsVecSize = buf.getInt(0x40);

			propsVec = new ArrayList<>();

			for (int i = 0; i < propsVecSize; i++) {
				propsVec.add(new VectorEntry(propsVecPtr + i * 4));
			}

			recvTable = RecvTable.fromAddr(rcvTablePtr);
			return this;
		}

		public ArrayList<VectorEntry> getPropsVec() {
			return propsVec;
		}

		public RecvTable getRecvTable() {
			return recvTable;
		}
	}

	public static class VectorEntry {
		private long ptr;

		public VectorEntry(long l) {
			MemoryBuffer buf = process.read(l, 4);
			ptr = Integer.toUnsignedLong(buf.getInt(0));
		}

		public long getPtr() {
			return ptr;
		}
	}

	public static class ServerClass {
		private String networkName;
		private SendTable sendTable;
		private ServerClass next;
		private int classID;
		private int instanceBaselineIndex;

		public ServerClass fromAddr(long addr) {

			MemoryBuffer buf = process.read(addr, 20);

			long namePtr = Integer.toUnsignedLong(buf.getInt(0));
			long sendTablePtr = Integer.toUnsignedLong(buf.getInt(4));
			long nextPtr = Integer.toUnsignedLong(buf.getInt(8));
			classID = buf.getInt(12);
			instanceBaselineIndex = buf.getInt(16);

			networkName = process.read(namePtr, 512).getString(0);
			sendTable = SendTable.fromAddr(sendTablePtr);

			if (nextPtr != 0) {
				next = new ServerClass().fromAddr(nextPtr);
			}
			return this;
		}

		public int getClassID() {
			return classID;
		}

		public int getInstanceBaselineIndex() {
			return instanceBaselineIndex;
		}

		public String getNetworkName() {
			return networkName;
		}

		public ServerClass getNext() {
			return next;
		}

		public SendTable getSendTable() {
			return sendTable;
		}
	}

	public static class LinkedListEntry {
		private short a;
		private short b;
		private long ptr;

		public LinkedListEntry(long addr) {
			MemoryBuffer buf = process.read(addr, 8);
			ptr = Integer.toUnsignedLong(buf.getInt(0));
			a = buf.getShort(4);
			b = buf.getShort(6);
		}

		public short getA() {
			return a;
		}

		public short getB() {
			return b;
		}

		public long getPtr() {
			return ptr;
		}
	}

	public static class RecvTable {
		private static final HashMap<Long, RecvTable> tablesCache = new HashMap<>();

		private long propPtr;
		private int nProps;
		private String tableName;

		public static RecvTable fromAddr(long recvTablePtr) {
			if (tablesCache.containsKey(recvTablePtr)) {
				return tablesCache.get(recvTablePtr);
			}
			return new RecvTable().fromAddrPriv(recvTablePtr);
		}

		private RecvTable fromAddrPriv(long recvTablePtr) {
			MemoryBuffer buf = process.read(recvTablePtr, 0x10);

			propPtr = Integer.toUnsignedLong(buf.getInt(0));
			nProps = buf.getInt(4);

			long namePtr = Integer.toUnsignedLong(buf.getInt(0xc));

			tableName = process.read(namePtr, 512).getString(0);
			tablesCache.put(recvTablePtr, this);
			recvTables.put(tableName, this);
			return this;
		}

		public int getnProps() {
			return nProps;
		}

		public String getTableName() {
			return tableName;
		}
	}

	public static class SendTable {
		private static final HashMap<Long, SendTable> tablesCache = new HashMap<>();

		private long propPtr;
		private SendProp[] props;
		private int nProps;
		private String tableName;

		public static SendTable fromAddr(long sendTablePtr) {
			if (tablesCache.containsKey(sendTablePtr)) {
				return tablesCache.get(sendTablePtr);
			}
			return new SendTable().fromAddrPriv(sendTablePtr);
		}

		private SendTable fromAddrPriv(long sendTablePtr) {
			MemoryBuffer buf = process.read(sendTablePtr, 0xD);

			propPtr = Integer.toUnsignedLong(buf.getInt(0));
			nProps = buf.getInt(4);
			props = new SendProp[nProps];
			long namePtr = Integer.toUnsignedLong(buf.getInt(8));
			for (int i = 0; i < nProps; i++) {
				SendProp prop = new SendProp().fromAddr(propPtr + i * SendProp.size);
				props[i] = prop;
			}
			tableName = process.read(namePtr, 512).getString(0);
			tablesCache.put(sendTablePtr, this);
			sendTables.put(tableName, this);
			return this;
		}

		public int getnProps() {
			return nProps;
		}

		public SendProp[] getProps() {
			return props;
		}

		public String getTableName() {
			return tableName;
		}
	}

	public static class SendProp {

		private static final int size = 0x54;
		private long recvPropPtr;
		private SendPropType type;
		private int nBits;

		private int priority;

		private float fLowValue;
		private float fHighValue;

		private int nElements;
		private int ElementStride;
		private String excludeDTName;

		private String varName;

		private SendProp template;

		private int flags;

		private int offset;

		private SendTable subTable;

		public SendTable getSubTable() {
			return subTable;
		}

		public String getVarName() {
			return varName;
		}

		public SendPropType getType() {
			return type;
		}

		public int getOffset() {
			return offset;
		}

		public int getFlags() {
			return flags;
		}

		public int getnBits() {
			return nBits;
		}

		public int getnElements() {
			return nElements;
		}

		public int getElementStride() {
			return ElementStride;
		}

		public float getfLowValue() {
			return fLowValue;
		}

		public float getfHighValue() {
			return fHighValue;
		}

		public SendProp fromAddr(long addr) {
			MemoryBuffer buf = process.read(addr, size);
			int t = buf.getInt(8);
			type = SendPropType.values()[t];
			nBits = buf.getInt(0xC);
			fLowValue = buf.getFloat(0x10);
			fHighValue = buf.getFloat(0x14);

			long templatePtr = 0;
			if (type == SendPropType.DPT_Array) {
				templatePtr = Integer.toUnsignedLong(buf.getInt(0x18));
			}

			nElements = buf.getInt(0x20);
			ElementStride = buf.getInt(0x24);

			long excludeNamePtr = Integer.toUnsignedLong(buf.getInt(0x28));

			long namePtr = Integer.toUnsignedLong(buf.getInt(0x30));

			flags = buf.getInt(0x3c);

			offset = buf.getInt(0x4c);
			long sendTablePtr = Integer.toUnsignedLong(buf.getInt(0x48));

			if (templatePtr != 0) {
				template = new SendProp().fromAddr(templatePtr);
			}

			if (namePtr != 0) {
				if (process.canRead(namePtr, 4)) {
					varName = process.read(namePtr, 128).getString(0);
				}
			}

			if (excludeNamePtr != 0 && (flags & (1 << 6)) != 0) {
				excludeDTName = process.read(excludeNamePtr, 128).getString(0);
			}


			if (sendTablePtr != 0 && type == SendPropType.DPT_DataTable) {
				subTable = SendTable.fromAddr(sendTablePtr);
			}

			return this;
		}

		public enum SendPropType {
			DPT_Int, DPT_Float, DPT_Vector, DPT_VectorXY, DPT_String, DPT_Array, DPT_DataTable, DPT_INT64,
			DPT_NUMSendPropTypes;
		}

		public int getPriority() {
			return priority;
		}

		public SendProp getTemplate() {
			return template;
		}

		@Override
		public String toString() {
			if (type == SendPropType.DPT_Array) {
				return template.toString() + " | " + varName + " -> " + String.format("0x%04x", offset) + " ["
						+ type.name() + "]";
			}
			return varName + " -> " + String.format("0x%04x", offset) + " [" + type.name() + "]";
		}
	}
}
