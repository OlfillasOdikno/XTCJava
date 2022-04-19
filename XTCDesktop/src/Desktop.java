import org.pcap4j.core.NotOpenException;
import org.pcap4j.core.PcapNativeException;

import application.Main;
import de.olfillasodikno.xtc.PassiveSniffer;
import de.olfillasodikno.xtc.modules.ModuleLoader;
import de.olfillasodikno.xtc.modules.radar.RadarModule;

public class Desktop {

	public static void main(String[] args) throws PcapNativeException, NotOpenException {
		Main.main(args);
	}

}
