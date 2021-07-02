/*******************************************************************************
 * Copyright (C) 2020 FVA GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.  You may obtain a copy
 * of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 ******************************************************************************/
package info.rexs.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import info.rexs.db.constants.RexsAttributeId;
import info.rexs.db.constants.RexsUnitId;
import info.rexs.model.jaxb.Array;
import info.rexs.model.jaxb.ArrayCodeType;
import info.rexs.model.jaxb.Attribute;
import info.rexs.model.jaxb.C;
import info.rexs.model.jaxb.Matrix;
import info.rexs.model.jaxb.ObjectFactory;
import info.rexs.model.jaxb.R;
import info.rexs.model.util.Base64Utils;

/**
 * This class represents an attribute of a REXS model.
 *
 * @author FVA GmbH
 */
public class RexsAttribute {

	/** The representation of this attribute in the JAXB model. */
	private Attribute rawAttribute;

	/** The ID of the attribute. */
	private RexsAttributeId attributeId;

	/**
	 * Constructs a new {@link RexsAttribute} for the given {@link Attribute}.
	 *
	 * @param rawAttribute
	 * 				The representation of this attribute in the JAXB model.
	 */
	public RexsAttribute(Attribute rawAttribute) {
		this.rawAttribute = rawAttribute;
		this.attributeId = RexsAttributeId.findById(rawAttribute.getId());
		Objects.requireNonNull(attributeId, "attribute id cannot be empty");
		RexsUnitId unitId = RexsUnitId.findById(rawAttribute.getUnit());
		Objects.requireNonNull(unitId, "unit cannot be empty");
		checkUnit(unitId);
	}

	/**
	 * Constructs a new {@link RexsAttribute} from scratch.
	 *
	 * @param attributeId
	 * 				The ID of the attribute.
	 */
	public RexsAttribute(RexsAttributeId attributeId) {
		this.attributeId = attributeId;
		this.rawAttribute = new Attribute();
		this.rawAttribute.setId(attributeId.getId());
		this.rawAttribute.setUnit(attributeId.getUnit().getId());
	}

	/**
	 * @return
	 * 				The ID of the attribute as a {@link RexsAttributeId}.
	 */
	public RexsAttributeId getAttributeId() {
		return attributeId;
	}

	/**
	 * @return
	 * 				The unit of the attribute as {@link RexsUnitId}.
	 */
	public RexsUnitId getUnit() {
		return attributeId.getUnit();
	}

	/**
	 * @return
	 * 				The representation of this attribute in the JAXB model.
	 */
	public Attribute getRawAttribute() {
		return rawAttribute;
	}

	/**
	 * Checks whether the attribute has a value.
	 *
	 * @return
	 * 				{@code true} if the attribute has a value, otherwise {@code false}.
	 */
	public boolean hasValue() {
		List<Object> valueContent = rawAttribute.getContent();
		if (valueContent == null || valueContent.isEmpty())
			return false;

		Object value = valueContent.get(0);
		if (value instanceof String)
			return !((String)value).isEmpty();

		Array array = readArrayElement();
		if (array != null)
			return hasValue(array);

		if (value instanceof Matrix) {
			Matrix matrixValue = (Matrix)value;
			if (matrixValue.getR() == null || matrixValue.getR().isEmpty())
				return false;
			List<String> matrixValueColumns = matrixValue.getR().get(0).getC();
			return !matrixValueColumns.isEmpty() && matrixValueColumns.get(0) != null;
		}

		return false;
	}

	private boolean hasValue(Array array) {
		if (array.getContent().isEmpty())
			return false;

		boolean hasCValue = array.getContent()
			.stream()
			.filter(C.class::isInstance)
			.map(C.class::cast)
			.map(C::getValue)
			.filter(Objects::nonNull)
			.anyMatch(value -> !value.isEmpty());

		if (hasCValue)
			return true;

		return array.getContent()
			.stream()
			.filter(String.class::isInstance)
			.map(String.class::cast)
			.filter(Objects::nonNull)
			.map(String::trim)
			.anyMatch(value -> !value.isEmpty());
	}

	/**
	 * Returns the string value of the attribute.
	 *
	 * @return
	 * 				The value of the attribute as {@link String}.
	 *
	 * @throws RexsModelAccessException
	 * 				If the attribute has no string value.
	 */
	public String getStringValue() {
		String value = readStringValue();

		if (value == null)
			throw new RexsModelAccessException(
					"string value cannot be null for attribute " + this.getAttributeId().getId());

		return value;
	}

	/**
	 * Returns the boolean value of the attribute.
	 *
	 * @return
	 * 				The value of the attribute as {@link boolean}.
	 *
	 * @throws RexsModelAccessException
	 * 				If the attribute has no boolean value.
	 */
	public boolean getBooleanValue() {
		String valueString = readStringValue();
		Boolean value = null;
		if (valueString != null && !valueString.isEmpty())
			value = Boolean.valueOf(valueString);

		if (value == null)
			throw new RexsModelAccessException(
					"boolean value cannot be null for attribute " + this.getAttributeId().getId());

		return value;
	}

	/**
	 * Returns the integer value of the attribute.
	 *
	 * @return
	 * 				The value of the attribute as {@link Integer}.
	 *
	 * @throws RexsModelAccessException
	 * 				If the attribute has no integer value.
	 */
	public int getIntegerValue() {
		String valueString = readStringValue();
		Integer value = null;
		if (valueString != null && !valueString.isEmpty()) {
			try {
				value = Integer.parseInt(valueString);
			} catch (NumberFormatException ex) {
				throw new RexsModelAccessException(
						"cannot read integer value " + valueString + " from attribute " + this.getAttributeId().getId(),
						ex);
			}
		}

		if (value == null)
			throw new RexsModelAccessException(
					"integer value cannot be null for attribute " + this.getAttributeId().getId());

		return value;
	}

	/**
	 * Returns the floating point value of the attribute.
	 *
	 * @param unit
	 * 				The unit of the attribute as {@link RexsUnitId}.
	 *
	 * @return
	 * 				The value of the attribute as {@link Double}.
	 *
	 * @throws RexsModelAccessException
	 * 				If the attribute has no double value or the unit does not match the unit of the attribute.
	 */
	public double getDoubleValue(RexsUnitId unit) {
		String valueString = readStringValue();
		Double value = null;
		if (valueString != null && !valueString.isEmpty()) {
			try {
				value = Double.parseDouble(valueString);
				if (Double.isNaN(value))
					value = null;
			} catch (NumberFormatException ex) {
				throw new RexsModelAccessException(
						"cannot read double value " + valueString + " from attribute " + this.getAttributeId().getId(),
						ex);
			}
		}

		if (value == null)
			throw new RexsModelAccessException(
					"double value cannot be null for attribute " + this.getAttributeId().getId());

		checkUnit(unit);

		return value;
	}

	/**
	 * Returns the string array value of the attribute.
	 *
	 * @return
	 * 				The value of the attribute as {@link String[]}.
	 *
	 * @throws RexsModelAccessException
	 * 				If the attribute has no string array value.
	 */
	public String[] getStringArrayValue() {
		List<String> valueString = readStringArrayValue();
		String [] value = null;
		if (valueString != null && !valueString.isEmpty())
			value = convertStringListToStringArray(valueString);

		if (value == null)
			throw new RexsModelAccessException(
					"string array value cannot be null for attribute " + this.getAttributeId().getId());

		return value;
	}

	/**
	 * Returns the boolean array value of the attribute.
	 *
	 * @return
	 * 				The value of the attribute as {@link Boolean[]}.
	 *
	 * @throws RexsModelAccessException
	 * 				If the attribute has no boolean array value.
	 */
	public Boolean[] getBooleanArrayValue() {
		List<String> valueString = readStringArrayValue();
		Boolean[] value = null;
		if (valueString != null && !valueString.isEmpty())
			value = convertStringListToBooleanArray(valueString);

		if (value == null)
			throw new RexsModelAccessException(
					"boolean array value cannot be null for attribute " + this.getAttributeId().getId());

		return value;
	}

	/**
	 * Returns the integer array value of the attribute.
	 *
	 * @return
	 * 				The value of the attribute as {@link Integer[]}.
	 *
	 * @throws RexsModelAccessException
	 * 				If the attribute has no integer array value.
	 */
	public Integer[] getIntegerArrayValue() {
		Integer[] value = null;

		ArrayCodeType arrayCode = readArrayCodeType();
		if (arrayCode == null) {
			List<String> valueString = readStringArrayValue();
			if (valueString != null && !valueString.isEmpty())
				value = convertStringListToIntegerArrayBoxed(valueString);

		} else if (arrayCode == ArrayCodeType.INT_32) {
			String base64 = readArrayBase64Value();
			value = Base64Utils.decodeInt32ArrayBoxed(base64);
		}

		if (value == null)
			throw new RexsModelAccessException(
					"integer array value cannot be null for attribute " + this.getAttributeId().getId());

		return value;
	}

	/**
	 * Returns the integer array value of the attribute.
	 *
	 * @return
	 * 				The value of the attribute as {@link int[]}.
	 *
	 * @throws RexsModelAccessException
	 * 				If the attribute has no integer array value.
	 */
	public int[] getIntegerArrayValueUnboxed() {
		int[] value = null;

		ArrayCodeType arrayCode = readArrayCodeType();
		if (arrayCode == null) {
			List<String> valueString = readStringArrayValue();
			if (valueString != null && !valueString.isEmpty())
				value = convertStringListToIntegerArrayUnboxed(valueString);

		} else if (arrayCode == ArrayCodeType.INT_32) {
			String base64 = readArrayBase64Value();
			value = Base64Utils.decodeInt32Array(base64);
		}

		if (value == null)
			throw new RexsModelAccessException(
					"integer array value cannot be null for attribute " + this.getAttributeId().getId());

		return value;
	}

	/**
	 * Returns the floating point array value of the attribute.
	 *
	 * @param unit
	 * 				The unit of the attribute as {@link RexsUnitId}.
	 *
	 * @return
	 * 				The value of the attribute as {@link Double[]}.
	 *
	 * @throws RexsModelAccessException
	 * 				If the attribute has no double array value or the unit does not match the unit of the attribute.
	 */
	public Double[] getDoubleArrayValue(RexsUnitId unit) {
		Double[] value = null;

		ArrayCodeType arrayCode = readArrayCodeType();
		if (arrayCode == null) {
			List<String> valueString = readStringArrayValue();
			if (valueString != null && !valueString.isEmpty())
				value = convertStringListToDoubleArrayBoxed(valueString);

		} else if (arrayCode == ArrayCodeType.FLOAT_32) {
			String base64 = readArrayBase64Value();
			value = Base64Utils.decodeFloat32ArrayBoxed(base64);

		} else if (arrayCode == ArrayCodeType.FLOAT_64) {
			String base64 = readArrayBase64Value();
			value = Base64Utils.decodeFloat64ArrayBoxed(base64);
		}

		if (value == null)
			throw new RexsModelAccessException(
					"double array value cannot be null for attribute " + this.getAttributeId().getId());

		checkUnit(unit);

		return value;
	}

	/**
	 * Returns the floating point array value of the attribute.
	 *
	 * @param unit
	 * 				The unit of the attribute as {@link RexsUnitId}.
	 *
	 * @return
	 * 				The value of the attribute as {@link double[]}.
	 *
	 * @throws RexsModelAccessException
	 * 				If the attribute has no double array value or the unit does not match the unit of the attribute.
	 */
	public double[] getDoubleArrayValueUnboxed(RexsUnitId unit) {
		double[] value = null;

		ArrayCodeType arrayCode = readArrayCodeType();
		if (arrayCode == null) {
			List<String> valueString = readStringArrayValue();
			if (valueString != null && !valueString.isEmpty())
				value = convertStringListToDoubleArrayUnboxed(valueString);

		} else if (arrayCode == ArrayCodeType.FLOAT_32) {
			String base64 = readArrayBase64Value();
			float[] floatArray = Base64Utils.decodeFloat32Array(base64);
			value = convertFloatArrayToDoubleArray(floatArray);

		} else if (arrayCode == ArrayCodeType.FLOAT_64) {
			String base64 = readArrayBase64Value();
			value = Base64Utils.decodeFloat64Array(base64);
		}

		if (value == null)
			throw new RexsModelAccessException(
					"double array value cannot be null for attribute " + this.getAttributeId().getId());

		checkUnit(unit);

		return value;
	}

	/**
	 * Returns the string matrix value of the attribute.
	 *
	 * @return
	 * 				The value of the attribute as {@link String[][]}.
	 *
	 * @throws RexsModelAccessException
	 * 				If the attribute has no string matrix value.
	 */
	public String[][] getStringMatrixValue() {
		List<List<String>> valueString = readStringMatrixValue();
		String[][] value = null;
		if (valueString != null && !valueString.isEmpty())
			value = convertStringMatrixToStringMatrix(valueString);

		if (value == null)
			throw new RexsModelAccessException(
					"string matrix value cannot be null for attribute " + this.getAttributeId().getId());

		return value;
	}

	/**
	 * Returns the boolean matrix value of the attribute.
	 *
	 * @return
	 * 				The value of the attribute as {@link Boolean[][]}.
	 *
	 * @throws RexsModelAccessException
	 * 				If the attribute has no boolean matrix value.
	 */
	public Boolean[][] getBooleanMatrixValue() {
		List<List<String>> valueString = readStringMatrixValue();
		Boolean[][] value = null;
		if (valueString != null && !valueString.isEmpty())
			value = convertStringMatrixToBooleanMatrix(valueString);

		if (value == null)
			throw new RexsModelAccessException(
					"boolean matrix value cannot be null for attribute " + this.getAttributeId().getId());

		return value;
	}

	/**
	 * Returns the integer matrix value of the attribute.
	 *
	 * @return
	 * 				The value of the attribute as {@link Integer[][]}.
	 *
	 * @throws RexsModelAccessException
	 * 				If the attribute has no integer matrix value.
	 */
	public Integer[][] getIntegerMatrixValue() {
		List<List<String>> valueString = readStringMatrixValue();
		Integer[][] value = null;
		if (valueString != null && !valueString.isEmpty())
			value = convertStringMatrixToIntegerMatrix(valueString);

		if (value == null)
			throw new RexsModelAccessException(
					"integer matrix value cannot be null for attribute " + this.getAttributeId().getId());

		return value;
	}

	/**
	 * Returns the floating point matrix value of the attribute.
	 *
	 * @param unit
	 * 				The unit of the attribute as {@link RexsUnitId}.
	 *
	 * @return
	 * 				The value of the attribute as {@link Double[][]}.
	 *
	 * @throws RexsModelAccessException
	 * 				If the attribute has no double matrix value or the unit does not match the unit of the attribute.
	 */
	public Double[][] getDoubleMatrixValue(RexsUnitId unit) {
		List<List<String>> valueString = readStringMatrixValue();
		Double[][] value = null;
		if (valueString != null && !valueString.isEmpty())
			value = convertStringMatrixToDoubleMatrix(valueString);

		if (value == null)
			throw new RexsModelAccessException(
					"double matrix value cannot be null for attribute " + this.getAttributeId().getId());

		checkUnit(unit);

		return value;
	}

	private String readStringValue() {
		List<Object> valueContent = rawAttribute.getContent();
		if (valueContent == null || valueContent.isEmpty())
			return null;

		Object value = valueContent.get(0);
		if (value instanceof String)
			return (String)value;
		return null;
	}

	private List<String> readStringArrayValue() {
		Array array = readArrayElement();
		if (array != null)
			return array.getContent()
					.stream()
					.filter(C.class::isInstance)
					.map(C.class::cast)
					.map(C::getValue)
					.collect(Collectors.toList());

		return Collections.emptyList();
	}

	private Array readArrayElement() {
		List<Object> valueContent = rawAttribute.getContent();
		if (valueContent == null || valueContent.isEmpty())
			return null;

		return valueContent
				.stream()
				.filter(Array.class::isInstance)
				.map(Array.class::cast)
				.findFirst()
				.orElse(null);
	}

	private ArrayCodeType readArrayCodeType() {
		Array array = readArrayElement();
		if (array != null)
			return array.getCode();

		return null;
	}

	private String readArrayBase64Value() {
		Array array = readArrayElement();
		if (array != null) {
			return array.getContent()
					.stream()
					.filter(String.class::isInstance)
					.map(String.class::cast)
					.filter(Objects::nonNull)
					.map(String::trim)
					.filter(val -> !val.isEmpty())
					.findFirst()
					.orElse(null);
		}

		return null;
	}

	private List<List<String>> readStringMatrixValue() {
		List<Object> valueContent = rawAttribute.getContent();
		if (valueContent == null || valueContent.isEmpty())
			return Collections.emptyList();

		List<List<String>> matrixValue = new ArrayList<>();
		Object value = valueContent.get(0);
		if (value instanceof Matrix) {
			Matrix matrix = (Matrix)value;
			for (R row : matrix.getR()) {
				matrixValue.add(row.getC());
			}
		}
		return matrixValue;
	}

	private String[] convertStringListToStringArray(List<String> stringList) {
		String[] stringArray = new String[stringList.size()];

		for (int i = 0; i < stringList.size(); i++) {
			stringArray[i] = stringList.get(i);
		}
		return stringArray;
	}

	private Boolean[] convertStringListToBooleanArray(List<String> stringList) {
		Boolean[] booleanArray = new Boolean[stringList.size()];

		for (int i = 0; i < stringList.size(); i++) {
			String stringValue = stringList.get(i);
			if (!stringValue.isEmpty())
				booleanArray[i] = Boolean.valueOf(stringValue);
		}
		return booleanArray;
	}

	private Integer[] convertStringListToIntegerArrayBoxed(List<String> stringList) {
		Integer[] integerArray = new Integer[stringList.size()];

		for (int i = 0; i < stringList.size(); i++) {
			String stringValue = stringList.get(i);
			if (!stringValue.isEmpty()) {
				try {
					integerArray[i] = Integer.parseInt(stringValue);
				} catch (NumberFormatException ex) {
					throw new RexsModelAccessException("cannot read integer value " + stringValue + " from attribute "
							+ this.getAttributeId().getId(), ex);
				}
			}
		}
		return integerArray;
	}

	private int[] convertStringListToIntegerArrayUnboxed(List<String> stringList) {
		int[] integerArray = new int[stringList.size()];

		for (int i = 0; i < stringList.size(); i++) {
			String stringValue = stringList.get(i);
			if (!stringValue.isEmpty()) {
				try {
					integerArray[i] = Integer.parseInt(stringValue);
				} catch (NumberFormatException ex) {
					throw new RexsModelAccessException("cannot read integer value " + stringValue + " from attribute "
							+ this.getAttributeId().getId(), ex);
				}
			}
		}
		return integerArray;
	}

	private Double[] convertStringListToDoubleArrayBoxed(List<String> stringList) {
		Double[] doubleArray = new Double[stringList.size()];

		for (int i = 0; i < stringList.size(); i++) {
			String stringValue = stringList.get(i);
			if (!stringValue.isEmpty()) {
				try {
					doubleArray[i] = Double.parseDouble(stringValue);
					if (Double.isNaN(doubleArray[i]))
						doubleArray[i] = null;
				} catch (NumberFormatException ex) {
					throw new RexsModelAccessException("cannot read double value " + stringValue + " from attribute "
							+ this.getAttributeId().getId(), ex);
				}
			}
		}
		return doubleArray;
	}

	private double[] convertStringListToDoubleArrayUnboxed(List<String> stringList) {
		double[] doubleArray = new double[stringList.size()];

		for (int i = 0; i < stringList.size(); i++) {
			String stringValue = stringList.get(i);
			if (!stringValue.isEmpty()) {
				try {
					double parsedDouble = Double.parseDouble(stringValue);
					if (!Double.isNaN(parsedDouble))
						doubleArray[i] = parsedDouble;
				} catch (NumberFormatException ex) {
					throw new RexsModelAccessException("cannot read double value " + stringValue + " from attribute "
							+ this.getAttributeId().getId(), ex);
				}
			}
		}
		return doubleArray;
	}

	private double[] convertFloatArrayToDoubleArray(float[] input) {
		if (input == null)
			return null;

		double[] output = new double[input.length];
		for (int i = 0; i < input.length; i++) {
			output[i] = input[i];
		}
		return output;
	}

	private String[][] convertStringMatrixToStringMatrix(List<List<String>> stringMatrix) {
		String[][] newStringMatrix = new String[stringMatrix.size()][stringMatrix.get(0).size()];

		for (int i = 0; i < stringMatrix.size(); i++) {
			List<String> stringArray = stringMatrix.get(i);
			for (int j = 0; j < stringArray.size(); j++) {
				newStringMatrix[i][j] = stringArray.get(j);
			}
		}
		return newStringMatrix;
	}

	private Boolean[][] convertStringMatrixToBooleanMatrix(List<List<String>> stringMatrix) {
		Boolean[][] booleanMatrix = new Boolean[stringMatrix.size()][stringMatrix.get(0).size()];

		for (int i = 0; i < stringMatrix.size(); i++) {
			List<String> stringArray = stringMatrix.get(i);
			for (int j = 0; j < stringArray.size(); j++) {
				String stringValue = stringArray.get(j);
				if (!stringValue.isEmpty()) {
					booleanMatrix[i][j] = Boolean.valueOf(stringValue);
				}
			}
		}
		return booleanMatrix;
	}

	private Integer[][] convertStringMatrixToIntegerMatrix(List<List<String>> stringMatrix) {
		Integer[][] doubleMatrix = new Integer[stringMatrix.size()][stringMatrix.get(0).size()];

		for (int i = 0; i < stringMatrix.size(); i++) {
			List<String> stringArray = stringMatrix.get(i);
			for (int j = 0; j < stringArray.size(); j++) {
				String stringValue = stringArray.get(j);
				if (!stringValue.isEmpty()) {
					try {
						doubleMatrix[i][j] = Integer.parseInt(stringValue);
					} catch (NumberFormatException ex) {
						throw new RexsModelAccessException("cannot read integer value " + stringValue
								+ " from attribute " + this.getAttributeId().getId(), ex);
					}
				}
			}
		}
		return doubleMatrix;
	}

	private Double[][] convertStringMatrixToDoubleMatrix(List<List<String>> stringMatrix) {
		Double[][] doubleMatrix = new Double[stringMatrix.size()][stringMatrix.get(0).size()];

		for (int i = 0; i < stringMatrix.size(); i++) {
			List<String> stringArray = stringMatrix.get(i);
			for (int j = 0; j < stringArray.size(); j++) {
				String stringValue = stringArray.get(j);
				if (!stringValue.isEmpty()) {
					try {
						doubleMatrix[i][j] = Double.parseDouble(stringValue);
						if (Double.isNaN(doubleMatrix[i][j]))
							doubleMatrix[i][j] = null;
					} catch (NumberFormatException ex) {
						throw new RexsModelAccessException("cannot read double value " + stringValue
								+ " from attribute " + this.getAttributeId().getId(), ex);
					}
				}
			}
		}
		return doubleMatrix;
	}

	private void checkUnit(RexsUnitId unitToCheck) {
		if (attributeId.getUnit() != unitToCheck && attributeId.getUnit() != RexsUnitId.UNKNOWN)
			throw new RexsModelAccessException(String.format("incompatible units (%s <-> %s) on %s attribute",
					attributeId.getUnit().getId(), unitToCheck.getId(), attributeId.getId()));
	}

	/**
	 * Sets the string value of the attribute.
	 *
	 * @param value
	 * 				The value of the attribute as {@link String}.
	 */
	public void setStringValue(String value) {
		rawAttribute.getContent().clear();
		rawAttribute.getContent().add(value);
	}

	/**
	 * Sets the boolean value of the attribute.
	 *
	 * @param value
	 * 				The value of the attribute as {@link Boolean}.
	 */
	public void setBooleanValue(boolean value) {
		setStringValue(Boolean.toString(value));
	}

	/**
	 * Sets the integer value of the attribute.
	 *
	 * @param value
	 * 				The value of the attribute as {@link Integer}.
	 */
	public void setIntegerValue(int value) {
		setStringValue(Integer.toString(value));
	}

	/**
	 * Sets the floating point value of the attribute.
	 *
	 * @param value
	 * 				The value of the attribute as {@link Double}.
	 */
	public void setDoubleValue(double value) {
		setStringValue(Double.toString(value));
	}

	private void setArrayValue(Object[] value) {
		if (value == null)
			return;

		ObjectFactory objectFactory = new ObjectFactory();
		Array array = objectFactory.createArray();
		for (int i = 0; i < value.length; i++) {
			C c = objectFactory.createC();
			if (value[i] == null)
				c.setValue("");
			else
				c.setValue(String.valueOf(value[i]));
			array.getContent().add(c);
		}

		rawAttribute.getContent().clear();
		rawAttribute.getContent().add(array);
	}

	private void setArrayValueBase64(String base64Value, ArrayCodeType codeType) {
		if (base64Value == null)
			return;

		ObjectFactory objectFactory = new ObjectFactory();
		Array array = objectFactory.createArray();
		array.setCode(codeType);
		array.getContent().add(base64Value);

		rawAttribute.getContent().clear();
		rawAttribute.getContent().add(array);
	}

	/**
	 * Sets the string array value of the attribute.
	 *
	 * @param value
	 * 				The value of the attribute as {@link String[]}.
	 */
	public void setStringArrayValue(String[] value) {
		setArrayValue(value);
	}

	/**
	 * Sets the bolean array value of the attribute.
	 *
	 * @param value
	 * 				The value of the attribute as {@link Boolean[]}.
	 */
	public void setBooleanArrayValue(Boolean[] value) {
		setArrayValue(value);
	}

	/**
	 * Sets the integer array value of the attribute.
	 *
	 * @param value
	 * 				The value of the attribute as {@link Integer[]}.
	 */
	public void setIntegerArrayValue(Integer[] value) {
		setArrayValue(value);
	}

	/**
	 * Sets the integer array value of the attribute.
	 *
	 * @param value
	 * 				The value of the attribute as {@link int[]}.
	 */
	public void setIntegerArrayValue(int[] value) {
		String base64Value = Base64Utils.encodeInt32Array(value);
		setArrayValueBase64(base64Value, ArrayCodeType.INT_32);
	}

	/**
	 * Sets the floating point array value of the attribute.
	 *
	 * @param value
	 * 				The value of the attribute as {@link Double[]}.
	 */
	public void setDoubleArrayValue(Double[] value) {
		setArrayValue(value);
	}

	/**
	 * Sets the floating point array value of the attribute.
	 *
	 * @param value
	 * 				The value of the attribute as {@link float[]}.
	 */
	public void setDoubleArrayValue(float[] value) {
		String base64Value = Base64Utils.encodeFloat32Array(value);
		setArrayValueBase64(base64Value, ArrayCodeType.FLOAT_32);
	}

	/**
	 * Sets the floating point array value of the attribute.
	 *
	 * @param value
	 * 				The value of the attribute as {@link double[]}.
	 */
	public void setDoubleArrayValue(double[] value) {
		String base64Value = Base64Utils.encodeFloat64Array(value);
		setArrayValueBase64(base64Value, ArrayCodeType.FLOAT_64);
	}

	private void setMatrixValue(Object[][] value) {
		if (value == null)
			return;

		ObjectFactory objectFactory = new ObjectFactory();
		Matrix matrix = objectFactory.createMatrix();
		for (int i = 0; i < value.length; i++) {
			R row = objectFactory.createR();

			if (value[i] != null) {
				for (int j = 0; j < value[i].length; j++) {
					if (value[i][j] == null)
						row.getC().add("");
					else
						row.getC().add(String.valueOf(value[i][j]));
				}
			}
			matrix.getR().add(row);
		}

		rawAttribute.getContent().clear();
		rawAttribute.getContent().add(matrix);
	}

	/**
	 * Sets the string matrix value of the attribute.
	 *
	 * @param value
	 * 				The value of the attribute as {@link String[][]}.
	 */
	public void setStringMatrixValue(String[][] value) {
		setMatrixValue(value);
	}

	/**
	 * Sets the boolean matrix value of the attribute.
	 *
	 * @param value
	 * 				The value of the attribute as {@link Boolean[][]}.
	 */
	public void setBooleanMatrixValue(Boolean[][] value) {
		setMatrixValue(value);
	}

	/**
	 * Sets the integer matrix value of the attribute.
	 *
	 * @param value
	 * 				The value of the attribute as {@link Integer[][]}.
	 */
	public void setIntegerMatrixValue(Integer[][] value) {
		setMatrixValue(value);
	}

	/**
	 * Sets the floating point matrix value of the attribute.
	 *
	 * @param value
	 * 				The value of the attribute as {@link Double[][]}.
	 */
	public void setDoubleMatrixValue(Double[][] value) {
		setMatrixValue(value);
	}
}
