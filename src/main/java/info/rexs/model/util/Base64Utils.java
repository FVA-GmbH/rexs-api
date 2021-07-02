package info.rexs.model.util;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.Arrays;
import java.util.Base64;
import java.util.stream.IntStream;

public class Base64Utils {

	private Base64Utils() {}

	public static String encodeInt32Array(int[] array) {
		return Base64.getEncoder().encodeToString(intToByteArray(array));
	}

	public static int[] decodeInt32Array(String base64Encoded) {
		if (isEmpty(base64Encoded))
			return null;
		byte[] byteArray = Base64.getDecoder().decode(base64Encoded);
		return byteToIntArray(byteArray);
	}

	public static Integer[] decodeInt32ArrayBoxed(String base64Encoded) {
		if (isEmpty(base64Encoded))
			return null;
		int[] intArray = decodeInt32Array(base64Encoded);
		return Arrays.stream(intArray).boxed().toArray(Integer[]::new);
	}

	public static String encodeFloat32Array(float[] array) {
		return Base64.getEncoder().encodeToString(floatToByteArray(array));
	}

	public static float[] decodeFloat32Array(String base64Encoded) {
		if (isEmpty(base64Encoded))
			return null;
		byte[] byteArray = Base64.getDecoder().decode(base64Encoded);
		return byteToFloatArray(byteArray);
	}

	public static Double[] decodeFloat32ArrayBoxed(String base64Encoded) {
		if (isEmpty(base64Encoded))
			return null;
		float[] floatArray = decodeFloat32Array(base64Encoded);
		return IntStream.range(0, floatArray.length).mapToDouble(i -> floatArray[i]).boxed().toArray(Double[]::new);
	}

	public static String encodeFloat64Array(double[] array) {
		return Base64.getEncoder().encodeToString(doubleToByteArray(array));
	}

	public static double[] decodeFloat64Array(String base64Encoded) {
		if (isEmpty(base64Encoded))
			return null;
		byte[] byteArray = Base64.getDecoder().decode(base64Encoded);
		return byteToDoubleArray(byteArray);
	}

	public static Double[] decodeFloat64ArrayBoxed(String base64Encoded) {
		if (isEmpty(base64Encoded))
			return null;
		double[] doubleArray = decodeFloat64Array(base64Encoded);
		return Arrays.stream(doubleArray).boxed().toArray(Double[]::new);
	}

	private static byte[] intToByteArray(int[] array) {
		ByteBuffer buf = ByteBuffer.allocate(Integer.SIZE / Byte.SIZE * array.length);
		buf.order(ByteOrder.LITTLE_ENDIAN).asIntBuffer().put(array);
		return buf.array();
	}

	private static byte[] floatToByteArray(float[] array) {
		ByteBuffer buf = ByteBuffer.allocate(Float.SIZE / Byte.SIZE * array.length);
		buf.order(ByteOrder.LITTLE_ENDIAN).asFloatBuffer().put(array);
		return buf.array();
	}

	private static byte[] doubleToByteArray(double[] array) {
		ByteBuffer buf = ByteBuffer.allocate(Double.SIZE / Byte.SIZE * array.length);
		buf.order(ByteOrder.LITTLE_ENDIAN).asDoubleBuffer().put(array);
		return buf.array();
	}

	private static int[] byteToIntArray(byte[] bytes) {
		IntBuffer buf = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).asIntBuffer();
		int[] array = new int[buf.limit()];
		buf.get(array);
		return array;
	}

	private static float[] byteToFloatArray(byte[] bytes) {
		FloatBuffer buf = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).asFloatBuffer();
		float[] array = new float[buf.limit()];
		buf.get(array);
		return array;
	}

	private static double[] byteToDoubleArray(byte[] bytes) {
		DoubleBuffer buf = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).asDoubleBuffer();
		double[] array = new double[buf.limit()];
		buf.get(array);
		return array;
	}

	private static boolean isEmpty(String value) {
		return value == null || value.length() < 1;
	}
}
