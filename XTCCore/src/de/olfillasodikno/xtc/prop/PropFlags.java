package de.olfillasodikno.xtc.prop;

public class PropFlags {
	PropFlags(){}
	public static final int UNSIGNED = 1 << 0;
	public static final int COORD = 1 << 1;
	public static final int NOSCALE = 1 << 2;
	public static final int ROUNDDOWN = 1 << 3;
	public static final int ROUNDUP = 1 << 4;
	public static final int NORMAL = 1 << 5;
	public static final int EXCLUDE = 1 << 6;
	public static final int XYZE = 1 << 7;
	public static final int INSIDEARRAY = 1 << 8;
	public static final int PROXY_ALWAYS_YES = 1 << 9;
	public static final int IS_A_VECTOR_ELEM = 1 << 10;
	public static final int COLLAPSIBLE = 1 << 11;
	public static final int COORD_MP = 1 << 12;
	public static final int COORD_MP_LOWPRECISION = 1 << 13;
	public static final int COORD_MP_INTEGRAL = 1 << 14;
	public static final int CELL_COORD = 1 << 15;
	public static final int CELL_COORD_LOWPRECISION = 1 << 16;
	public static final int CELL_COORD_INTEGRAL = 1 << 17;
	public static final int CHANGES_OFTEN = 1 << 18;
	public static final int VARINT = 1 << 19;
}
