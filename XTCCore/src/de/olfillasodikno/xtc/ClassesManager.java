package de.olfillasodikno.xtc;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import de.olfillasodikno.xtc.ClassesManager.SendProp.SendPropType;
import de.olfillasodikno.xtc.prop.PropFlags;

public class ClassesManager {

	public static File DEFAULT_ICE_KEY_FILE = new File("ice_key");
	public static File DEFAULT_SERVER_DUMP_FILE = new File("server_dump");
	private Map<Integer, ServerClassInfo> serverClasses = new HashMap<>();

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

	public static byte[] readAllBytes(File f) throws IOException {
		byte[] data = new byte[(int) f.length()];
		FileInputStream fis = new FileInputStream(f);
		fis.read(data);
		fis.close();
		return data;
	}

	public ClassesManager() {
		try {
			iceKey = readAllBytes(DEFAULT_ICE_KEY_FILE);

		} catch (IOException e) {
			e.printStackTrace();
		}
		try {
			byte[] data = readAllBytes(DEFAULT_SERVER_DUMP_FILE);
			ByteBuffer buf = ByteBuffer.wrap(data);
			read(buf);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void read(ByteBuffer buf) {
		readServerClasses(buf);
		readFlatTables(buf);
	}

	private void readFlatTables(ByteBuffer buf) {
		int size = buf.getInt();
		for (int i = 0; i < size; i++) {
			String key = readString(buf);
			ArrayList<SendProp> value = readSendProps(buf);
			flatTables.put(key, value);
		}
	}

	private ArrayList<SendProp> readSendProps(ByteBuffer buf) {
		ArrayList<SendProp> ret = new ArrayList<>();
		int size = buf.getInt();
		for (int i = 0; i < size; i++) {
			SendProp sp = readSendProp(buf);
			ret.add(sp);
		}
		return ret;
	}

	private SendProp readSendProp(ByteBuffer buf) {
		SendProp ret = new SendProp();
		ret.type = SendPropType.values()[buf.getInt()];
		ret.nBits = buf.getInt();
		ret.fLowValue = buf.getFloat();
		ret.fHighValue = buf.getFloat();
		ret.nElements = buf.getInt();
		ret.ElementStride = buf.getInt();
		ret.flags = buf.getInt();

		if (ret.type == SendPropType.DPT_Array) {
			ret.template = readSendProp(buf);
		}
		ret.varName = readString(buf);

		if ((ret.flags & PropFlags.EXCLUDE) != 0) {
			ret.excludeDTName = readString(buf);
		}

		if (ret.type == SendPropType.DPT_DataTable) {
			ret.subTable = readDataTable(buf);
		}
		return ret;
	}

	private SendTable readDataTable(ByteBuffer buf) {
		SendTable ret = new SendTable();
		ret.nProps = buf.getInt();
		for (int i = 0; i < ret.nProps; i++) {
			ret.props[i] = readSendProp(buf);
		}
		ret.tableName = readString(buf);
		return ret;
	}

	private void readServerClasses(ByteBuffer buf) {
		int size = buf.getInt();
		for (int i = 0; i < size; i++) {
			int key = buf.getInt();
			ServerClassInfo value = readServerClassInfo(buf);
			serverClasses.put(key, value);
		}
	}

	private ServerClassInfo readServerClassInfo(ByteBuffer buf) {
		ServerClassInfo ret = new ServerClassInfo();
		ret.className = readString(buf);
		ret.datatableName = readString(buf);
		ret.instanceBaselineIndex = buf.getInt();
		return ret;
	}

	private String readString(ByteBuffer buf) {
		int length = buf.getInt();
		byte[] data = new byte[length];
		buf.get(data);
		return new String(data);
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

	public static class ServerClass {
		private String networkName;
		private SendTable sendTable;
		private ServerClass next;
		private int classID;
		private int instanceBaselineIndex;

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

	public static class SendTable {

		private long propPtr;
		private SendProp[] props;
		private int nProps;
		private String tableName;

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
