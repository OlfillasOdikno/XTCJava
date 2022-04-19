package de.olfillasodikno.bitutils.tests;

import java.util.Random;

import de.olfillasodikno.bitutils.BitReader;
import de.olfillasodikno.bitutils.BitWriter;

public class Test {

	public static void main(String[] args) {
		testUBitInt();
		testBitVar();
		testVarInt();
	}


	public static void testVarInt() {
		System.out.println("Testing VarInt..");
		int size = 1000;
		int[] values = new int[size];
		Random rnd = new Random();

		for (int i = 0; i < size; i++) {
			int v = rnd.nextInt();
			values[i] = v;
		}

		BitWriter bw = new BitWriter();
		for (int i = 0; i < size; i++) {
			bw.writeVarInt(values[i]);
		}

		boolean fail = false;
		BitReader br = new BitReader(bw.toByteArray());
		for (int i = 0; i < size; i++) {
			if (values[i] != br.readVarInt()) {
				System.err.println("Failed!");
				fail = true;
				break;
			}
		}
		if (!fail) {
			System.out.println("Success!");
		}
	}

	public static void testBitVar() {
		System.out.println("Testing BitVar..");
		int size = 1000;
		int[] values = new int[size];
		Random rnd = new Random();

		for (int i = 0; i < size; i++) {
			int v = rnd.nextInt();
			values[i] = v;
		}

		BitWriter bw = new BitWriter();
		for (int i = 0; i < size; i++) {
			bw.writeBitVar(values[i]);
		}

		boolean fail = false;
		BitReader br = new BitReader(bw.toByteArray());
		for (int i = 0; i < size; i++) {
			if (values[i] != br.readUBitVar()) {
				System.err.println("Failed!");
				fail = true;
				break;
			}
		}
		if (!fail) {
			System.out.println("Success!");
		}
	}

	public static void testUBitInt() {
		System.out.println("Testing UBitInt..");
		int size = 1000;
		int[] values = new int[size];
		int[] lengths = new int[size];
		Random rnd = new Random();

		for (int i = 0; i < size; i++) {
			int l = rnd.nextInt(31) + 1;
			int v = rnd.nextInt(1 << (l - 1));
			values[i] = v;
			lengths[i] = l;
		}

		BitWriter bw = new BitWriter();
		for (int i = 0; i < size; i++) {
			bw.writeUBitInt(values[i], lengths[i]);
		}

		boolean fail = false;
		BitReader br = new BitReader(bw.toByteArray());
		for (int i = 0; i < size; i++) {
			int v = br.readUBitInt(lengths[i]);
			if (v != values[i]) {
				System.err.println("Failed!");
				fail = true;
				break;
			}
		}
		if (!fail) {
			System.out.println("Success!");
		}
	}

}
