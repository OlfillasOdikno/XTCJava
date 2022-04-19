package de.olfillasodikno.xtc.prop.decoder;

import de.olfillasodikno.bitutils.BitReader;
import de.olfillasodikno.xtc.ClassesManager.SendProp;
import static de.olfillasodikno.xtc.prop.PropFlags.*;

public class PropDecoder {
	public static Object decode(SendProp prop, BitReader br) {
		switch (prop.getType()) {
		case DPT_Array:
			return decodeArray(prop, br);
		case DPT_DataTable:
			break;
		case DPT_Float:
			return decodeFloat(prop, br);
		case DPT_INT64:
			return decodeLong(prop, br);
		case DPT_Int:
			return decodeInt(prop, br);
		case DPT_NUMSendPropTypes:
			break;
		case DPT_String:
			return decodeString(prop, br);
		case DPT_Vector:
			return decodeVector(prop, br, new Vector());
		case DPT_VectorXY:
			return decodeVectorXY(prop, br, new Vector());
		default:
			break;
		}
		return null;
	}

	private static Object decodeVectorXY(SendProp prop, BitReader br, Vector v) {
		v.setX(decodeFloat(prop, br));
		v.setY(decodeFloat(prop, br));
		return v;
	}

	private static Object decodeVector(SendProp prop, BitReader br, Vector v) {
		decodeVectorXY(prop, br, v);
		if ((prop.getFlags() & NORMAL) == 0) {
			v.setZ(decodeFloat(prop, br));
		} else {
			int signbit = br.readBit();
			v.setZ(0);
			float l = v.lengthSQ();
			if (l < 1.0f) {
				float z = (float) Math.sqrt(1.0f - l);
				if (signbit != 0) {
					z *= -1;
				}
				v.setZ(z);
			}
		}
		return v;
	}

	private static final int DT_MAX_STRING_BITS = 9;
	private static final int DT_MAX_STRING_BUFFERSIZE = (1 << DT_MAX_STRING_BITS);

	private static String decodeString(SendProp prop, BitReader br) {
		int length = br.readUBitInt(DT_MAX_STRING_BITS);
		if (length >= DT_MAX_STRING_BUFFERSIZE) {
			length = DT_MAX_STRING_BUFFERSIZE - 1;
		}
		byte[] data = new byte[length];
		br.readByteArray(data, 0, length * 8);
		return new String(data);
	}

	private static float decodeFloat(SendProp prop, BitReader br) {
		if ((prop.getFlags() & COORD) != 0) {
			return readBitCoord(br);
		} else if ((prop.getFlags() & COORD_MP) != 0) {
			return readBitCoordMP(br, false, false);
		} else if ((prop.getFlags() & COORD_MP_LOWPRECISION) != 0) {
			return readBitCoordMP(br, false, true);
		} else if ((prop.getFlags() & COORD_MP_INTEGRAL) != 0) {
			return readBitCoordMP(br, true, false);
		} else if ((prop.getFlags() & NOSCALE) != 0) {
			return readBitFloat(br);
		} else if ((prop.getFlags() & NORMAL) != 0) {
			return readBitNormal(br);
		} else if ((prop.getFlags() & CELL_COORD) != 0) {
			return readBitCellCoord(br, prop.getnBits(), false, false);
		} else if ((prop.getFlags() & CELL_COORD_LOWPRECISION) != 0) {
			return readBitCellCoord(br, prop.getnBits(), true, false);
		} else if ((prop.getFlags() & CELL_COORD_INTEGRAL) != 0) {
			return readBitCellCoord(br, prop.getnBits(), false, true);
		}
		int interp = br.readUBitInt(prop.getnBits());
		float ret = (float) interp / ((1 << prop.getnBits()) - 1);
		ret = prop.getfLowValue() + (prop.getfHighValue() - prop.getfLowValue()) * ret;
		return ret;
	}

	private static final int COORD_INTEGER_BITS = 14;
	private static final int COORD_FRACTIONAL_BITS = 5;
	private static final int COORD_DENOMINATOR = (1 << (COORD_FRACTIONAL_BITS));
	private static final float COORD_RESOLUTION = (1.0f / (COORD_DENOMINATOR));

	private static final int COORD_INTEGER_BITS_MP = 11;
	private static final int COORD_FRACTIONAL_BITS_MP_LOWPRECISION = 3;
	private static final int COORD_DENOMINATOR_LOWPRECISION = (1 << (COORD_FRACTIONAL_BITS_MP_LOWPRECISION));
	private static final float COORD_RESOLUTION_LOWPRECISION = (1.0f / (COORD_DENOMINATOR_LOWPRECISION));

	private static final int NORMAL_FRACTIONAL_BITS = 11;
	private static final int NORMAL_DENOMINATOR = ((1 << (NORMAL_FRACTIONAL_BITS)) - 1);
	private static final float NORMAL_RESOLUTION = (1.0f / (NORMAL_DENOMINATOR));

	private static float readBitNormal(BitReader br) {
		int signbit = br.readBit();
		int fracVal = br.readUBitInt(NORMAL_FRACTIONAL_BITS);
		float ret = fracVal * NORMAL_RESOLUTION;
		if (signbit != 0) {
			ret *= -1;
		}
		return ret;
	}

	private static float readBitFloat(BitReader br) {
		int v = br.readUBitInt(32);
		return Float.intBitsToFloat(v);
	}

	private static float readBitCoord(BitReader br) {
		float ret = 0.0f;
		int intval = 0;
		int fracval = 0;
		int signbit = 0;

		intval = br.readBit();
		fracval = br.readBit();
		if (intval != 0 || fracval != 0) {
			signbit = br.readBit();

			if (intval != 0) {
				intval = br.readUBitInt(COORD_INTEGER_BITS) + 1;
			}
			if (fracval != 0) {
				fracval = br.readUBitInt(COORD_FRACTIONAL_BITS);
			}
			ret = intval + ((float) fracval * COORD_RESOLUTION);
			if (signbit != 0) {
				ret *= -1;
			}
		}
		return ret;
	}

	private static float readBitCoordMP(BitReader br, boolean integral, boolean lowPrecision) {
		float ret = 0.0f;
		int intval = 0;
		int fracval = 0;
		int signbit = 0;

		boolean inBounds = br.readBit() != 0;
		intval = br.readBit();
		if (integral) {
			if (intval != 0) {
				signbit = br.readBit();

				if (inBounds) {
					ret = (float) br.readUBitLong(COORD_INTEGER_BITS_MP) + 1;
				} else {
					ret = (float) br.readUBitLong(COORD_INTEGER_BITS) + 1;
				}
			}
		} else {
			signbit = br.readBit();
			if (intval != 0) {
				if (inBounds) {
					intval = br.readUBitInt(COORD_INTEGER_BITS_MP) + 1;
				} else {
					intval = br.readUBitInt(COORD_INTEGER_BITS) + 1;
				}
				fracval = br.readUBitInt(lowPrecision ? COORD_FRACTIONAL_BITS_MP_LOWPRECISION : COORD_FRACTIONAL_BITS);
				ret = intval + fracval * (lowPrecision ? COORD_RESOLUTION_LOWPRECISION : COORD_RESOLUTION);
			}
		}
		if (signbit != 0) {
			ret *= -1;
		}
		return ret;
	}

	private static float readBitCellCoord(BitReader br, int bits, boolean integral, boolean lowPrecision) {
		float ret = 0.0f;
		int intval = 0;
		int fracval = 0;
		if (integral) {
			ret = (float) br.readUBitLong(bits);
		} else {

			intval = br.readUBitInt(bits);

			fracval = br.readUBitInt(lowPrecision ? COORD_FRACTIONAL_BITS_MP_LOWPRECISION : COORD_FRACTIONAL_BITS);
			ret = intval + fracval * (lowPrecision ? COORD_RESOLUTION_LOWPRECISION : COORD_RESOLUTION);
		}
		return ret;
	}

	private static long decodeLong(SendProp prop, BitReader br) {
		if ((prop.getFlags() & VARINT) != 0) {
			if ((prop.getFlags() & UNSIGNED) != 0) {
				return br.readVarLong();
			} else {
				return br.readSignedVarLong();
			}
		} else {
			if ((prop.getFlags() & UNSIGNED) != 0) {
				return br.readUBitLong(prop.getnBits());
			} else {
				return br.readSBitLong(prop.getnBits());
			}
		}
	}

	private static Object decodeInt(SendProp prop, BitReader br) {
		if ((prop.getFlags() & VARINT) != 0) {
			if ((prop.getFlags() & UNSIGNED) != 0) {
				return br.readVarInt();
			} else {
				return br.readSignedVarInt();
			}
		} else {
			if ((prop.getFlags() & UNSIGNED) != 0) {
				return br.readUBitInt(prop.getnBits());
			} else {
				return br.readSBitInt(prop.getnBits());
			}
		}
	}

	private static Object decodeArray(SendProp prop, BitReader br) {
		int maxElements = prop.getnElements();
		int numBits = 1;
		while ((maxElements >>= 1) != 0) {
			numBits++;
		}
		int nElements = br.readUBitInt(numBits);
		Object[] ret = new Object[nElements];
		for (int i = 0; i < nElements; i++) {
			Object o = decode(prop.getTemplate(), br);
			ret[i]= o;
		}
		return null;
	}
}
