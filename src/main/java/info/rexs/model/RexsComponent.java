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

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.stream.Collectors;

import info.rexs.db.constants.RexsAttributeId;
import info.rexs.db.constants.RexsComponentType;
import info.rexs.model.jaxb.Attribute;
import info.rexs.model.jaxb.Component;
import lombok.EqualsAndHashCode;

/**
 * This class represents an component of a REXS model.
 *
 * @author FVA GmbH
 */
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class RexsComponent implements Comparable<RexsComponent> {

	/** The representation of this component in the JAXB model. */
	private Component rawComponent;

	/** The numeric ID of the component within the REXS model. */
	@EqualsAndHashCode.Include
	private Integer id;

	/** The type of the component. */
	private RexsComponentType type;

	/** An internal index with all attributes of the component for quick access. */
	private Map<String, RexsAttribute> attributes;

	/**
	 * Constructs a new {@link RexsComponent} for the given {@link Component}.
	 *
	 * @param rawComponent
	 * 				The representation of this component in the JAXB model.
	 */
	public RexsComponent(Component rawComponent) {
		this.rawComponent = rawComponent;
		this.id = rawComponent.getId();
		this.type = RexsComponentType.findById(rawComponent.getType());
		this.attributes = rawComponent.getAttribute().stream().collect(Collectors.toMap(Attribute::getId, RexsAttribute::new));
	}

	/**
	 * @return
	 * 				The numeric ID of the component within the REXS model.
	 */
	public Integer getId() {
		return id;
	}

	/**
	 * @return
	 * 				The name of the component within the REXS model.
	 */
	public String getName() {
		String name = rawComponent.getName();
		if (name == null)
			name = "";
		return name;
	}

	/**
	 * @return
	 * 				The type of the component as {@link RexsComponentType}.
	 */
	public RexsComponentType getType() {
		return type;
	}

	/**
	 * @return
	 * 				The representation of this component in the JAXB model.
	 */
	public Component getRawComponent() {
		return rawComponent;
	}

	/**
	 * @return
	 * 				All attributes assigned to the component as a {@link Collection} of {@link RexsAttribute}.
	 */
	public Collection<RexsAttribute> getAttributes() {
		return Collections.unmodifiableCollection(attributes.values());
	}

	/**
	 * Checks on the ID of an attribute whether the component contains a corresponding attribute.
	 *
	 * @param attributeId
	 * 				The ID of the attribute as {@link RexsAttributeId}.
	 *
	 * @return
	 * 				{@code true} if the component contains the attribute, otherwise {@code false}.
	 */
	public boolean hasAttribute(RexsAttributeId attributeId) {
		return hasAttribute(attributeId.getId());
	}

	/**
	 * Checks on the ID of an attribute whether the component contains a corresponding attribute.
	 *
	 * @param attributeId
	 * 				The ID of the attribute as {@link String}.
	 *
	 * @return
	 * 				{@code true} if the component contains the attribute, otherwise {@code false}.
	 */
	public boolean hasAttribute(String attributeId) {
		return attributes.containsKey(attributeId);
	}

	/**
	 * Returns the attribute of the component for an attribute ID.
	 *
	 * @param attributeId
	 * 				The ID of the attribute as {@link RexsAttributeId}.
	 *
	 * @return
	 * 				The attribute as {@link RexsAttribute} or {@code null} if the component
	 * 				does not contain a corresponding attribute.
	 *
	 * @throws RexsModelAccessException
	 * 				If the attribute is not defined on the component.
	 */
	public RexsAttribute getAttribute(RexsAttributeId attributeId) {
		return getAttribute(attributeId.getId());
	}

	/**
	 * Returns the attribute of the component for an attribute ID.
	 *
	 * @param attributeId
	 * 				The ID of the attribute as {@link String}.
	 *
	 * @return
	 * 				The attribute as {@link RexsAttribute} or {@code null} if the component
	 * 				does not contain a corresponding attribute.
	 *
	 * @throws RexsModelAccessException
	 * 				If the attribute is not defined on the component.
	 */
	public RexsAttribute getAttribute(String attributeId) {
		RexsAttribute rexsAttribute = attributes.get(attributeId);
		if (rexsAttribute == null)
			throw new RexsModelAccessException(this, "attribute '" + attributeId + "' not found!");
		return rexsAttribute;
	}

	/**
	 * Adds an attribute to the component.
	 * <p>
	 * If the component already has an attribute with the attribute ID, then the existing attribute is replaced by the new attribute.
	 *
	 * @param newAttribute
	 * 				The new attribute as {@link RexsAttribute}.
	 */
	public void addAttribute(RexsAttribute newAttribute) {
		if (attributes.containsKey(newAttribute.getAttributeId().getId())) {
			Iterator<Attribute> iterator = rawComponent.getAttribute().iterator();
			while(iterator.hasNext()) {
				Attribute attribute = iterator.next();
				if (attribute.getId().equals(newAttribute.getAttributeId().getId())) {
					iterator.remove();
					break;
				}
			}
		}
		attributes.put(newAttribute.getAttributeId().getId(), newAttribute);
		rawComponent.getAttribute().add(newAttribute.getRawAttribute());
	}

	private RexsAttribute createAndAddAttribute(RexsAttributeId attributeId) {
		RexsAttribute attribute = new RexsAttribute(attributeId);
		addAttribute(attribute);
		return attribute;
	}

	/**
	 * Creates a new attribute with a string value and adds it to the component.
	 * <p>
	 * If the component already has an attribute with the attribute ID, then the existing attribute is replaced by the new attribute.
	 *
	 * @param attributeId
	 * 				The ID of the new attribute as {@link RexsAttributeId}.
	 * @param value
	 * 				The value of the new attribute as {@link String}.
	 */
	public void addAttribute(RexsAttributeId attributeId, String value) {
		RexsAttribute attribute = createAndAddAttribute(attributeId);
		attribute.setStringValue(value);
	}

	/**
	 * Creates a new attribute with a boolean value and adds it to the component.
	 * <p>
	 * If the component already has an attribute with the attribute ID, then the existing attribute is replaced by the new attribute.
	 *
	 * @param attributeId
	 * 				The ID of the new attribute as {@link RexsAttributeId}.
	 * @param value
	 * 				The value of the new attribute as {@link Boolean}.
	 */
	public void addAttribute(RexsAttributeId attributeId, Boolean value) {
		RexsAttribute attribute = createAndAddAttribute(attributeId);
		attribute.setBooleanValue(value);
	}

	/**
	 * Creates a new attribute with a integer value and adds it to the component.
	 * <p>
	 * If the component already has an attribute with the attribute ID, then the existing attribute is replaced by the new attribute.
	 *
	 * @param attributeId
	 * 				The ID of the new attribute as {@link RexsAttributeId}.
	 * @param value
	 * 				The value of the new attribute as {@link Integer}.
	 */
	public void addAttribute(RexsAttributeId attributeId, Integer value) {
		RexsAttribute attribute = createAndAddAttribute(attributeId);
		attribute.setIntegerValue(value);
	}

	/**
	 * Creates a new attribute with a floating point value and adds it to the component.
	 * <p>
	 * If the component already has an attribute with the attribute ID, then the existing attribute is replaced by the new attribute.
	 *
	 * @param attributeId
	 * 				The ID of the new attribute as {@link RexsAttributeId}.
	 * @param value
	 * 				The value of the new attribute as {@link Double}.
	 */
	public void addAttribute(RexsAttributeId attributeId, Double value) {
		RexsAttribute attribute = createAndAddAttribute(attributeId);
		attribute.setDoubleValue(value);
	}

	/**
	 * Creates a new attribute with a string array value and adds it to the component.
	 * <p>
	 * If the component already has an attribute with the attribute ID, then the existing attribute is replaced by the new attribute.
	 *
	 * @param attributeId
	 * 				The ID of the new attribute as {@link RexsAttributeId}.
	 * @param arrayValues
	 * 				The value of the new attribute as {@link String[]}.
	 */
	public void addAttribute(RexsAttributeId attributeId, String[] arrayValues) {
		RexsAttribute attribute = createAndAddAttribute(attributeId);
		attribute.setStringArrayValue(arrayValues);
	}

	/**
	 * Creates a new attribute with a boolean array value and adds it to the component.
	 * <p>
	 * If the component already has an attribute with the attribute ID, then the existing attribute is replaced by the new attribute.
	 *
	 * @param attributeId
	 * 				The ID of the new attribute as {@link RexsAttributeId}.
	 * @param arrayValues
	 * 				The value of the new attribute as {@link Boolean[]}.
	 */
	public void addAttribute(RexsAttributeId attributeId, Boolean[] arrayValues) {
		RexsAttribute attribute = createAndAddAttribute(attributeId);
		attribute.setBooleanArrayValue(arrayValues);
	}

	/**
	 * Creates a new attribute with a integer array value and adds it to the component.
	 * <p>
	 * If the component already has an attribute with the attribute ID, then the existing attribute is replaced by the new attribute.
	 *
	 * @param attributeId
	 * 				The ID of the new attribute as {@link RexsAttributeId}.
	 * @param arrayValues
	 * 				The value of the new attribute as {@link Integer[]}.
	 */
	public void addAttribute(RexsAttributeId attributeId, Integer[] arrayValues) {
		RexsAttribute attribute = createAndAddAttribute(attributeId);
		attribute.setIntegerArrayValue(arrayValues);
	}

	/**
	 * Creates a new attribute with a integer array value and adds it to the component.
	 * <p>
	 * If the component already has an attribute with the attribute ID, then the existing attribute is replaced by the new attribute.
	 *
	 * @param attributeId
	 * 				The ID of the new attribute as {@link RexsAttributeId}.
	 * @param arrayValues
	 * 				The value of the new attribute as {@link int[]}.
	 */
	public void addAttribute(RexsAttributeId attributeId, int[] arrayValues) {
		RexsAttribute attribute = createAndAddAttribute(attributeId);
		attribute.setIntegerArrayValue(arrayValues);
	}

	/**
	 * Creates a new attribute with a floating point array value and adds it to the component.
	 * <p>
	 * If the component already has an attribute with the attribute ID, then the existing attribute is replaced by the new attribute.
	 *
	 * @param attributeId
	 * 				The ID of the new attribute as {@link RexsAttributeId}.
	 * @param arrayValues
	 * 				The value of the new attribute as {@link Double[]}.
	 */
	public void addAttribute(RexsAttributeId attributeId, Double[] arrayValues) {
		RexsAttribute attribute = createAndAddAttribute(attributeId);
		attribute.setDoubleArrayValue(arrayValues);
	}

	/**
	 * Creates a new attribute with a floating point array value and adds it to the component.
	 * <p>
	 * If the component already has an attribute with the attribute ID, then the existing attribute is replaced by the new attribute.
	 *
	 * @param attributeId
	 * 				The ID of the new attribute as {@link RexsAttributeId}.
	 * @param arrayValues
	 * 				The value of the new attribute as {@link float[]}.
	 */
	public void addAttribute(RexsAttributeId attributeId, float[] arrayValues) {
		RexsAttribute attribute = createAndAddAttribute(attributeId);
		attribute.setDoubleArrayValue(arrayValues);
	}

	/**
	 * Creates a new attribute with a floating point array value and adds it to the component.
	 * <p>
	 * If the component already has an attribute with the attribute ID, then the existing attribute is replaced by the new attribute.
	 *
	 * @param attributeId
	 * 				The ID of the new attribute as {@link RexsAttributeId}.
	 * @param arrayValues
	 * 				The value of the new attribute as {@link double[]}.
	 */
	public void addAttribute(RexsAttributeId attributeId, double[] arrayValues) {
		RexsAttribute attribute = createAndAddAttribute(attributeId);
		attribute.setDoubleArrayValue(arrayValues);
	}

	/**
	 * Creates a new attribute with a string matrix value and adds it to the component.
	 * <p>
	 * If the component already has an attribute with the attribute ID, then the existing attribute is replaced by the new attribute.
	 *
	 * @param attributeId
	 * 				The ID of the new attribute as {@link RexsAttributeId}.
	 * @param matrixValues
	 * 				The value of the new attribute as {@link String[][]}.
	 */
	public void addAttribute(RexsAttributeId attributeId, String[][] matrixValues) {
		RexsAttribute attribute = createAndAddAttribute(attributeId);
		attribute.setStringMatrixValue(matrixValues);
	}

	/**
	 * Creates a new attribute with a boolean matrix value and adds it to the component.
	 * <p>
	 * If the component already has an attribute with the attribute ID, then the existing attribute is replaced by the new attribute.
	 *
	 * @param attributeId
	 * 				The ID of the new attribute as {@link RexsAttributeId}.
	 * @param matrixValues
	 * 				The value of the new attribute as {@link Boolean[][]}.
	 */
	public void addAttribute(RexsAttributeId attributeId, Boolean[][] matrixValues) {
		RexsAttribute attribute = createAndAddAttribute(attributeId);
		attribute.setBooleanMatrixValue(matrixValues);
	}

	/**
	 * Creates a new attribute with a integer matrix value and adds it to the component.
	 * <p>
	 * If the component already has an attribute with the attribute ID, then the existing attribute is replaced by the new attribute.
	 *
	 * @param attributeId
	 * 				The ID of the new attribute as {@link RexsAttributeId}.
	 * @param matrixValues
	 * 				The value of the new attribute as {@link Integer[][]}.
	 */
	public void addAttribute(RexsAttributeId attributeId, Integer[][] matrixValues) {
		RexsAttribute attribute = createAndAddAttribute(attributeId);
		attribute.setIntegerMatrixValue(matrixValues);
	}

	/**
	 * Creates a new attribute with a floating point matrix value and adds it to the component.
	 * <p>
	 * If the component already has an attribute with the attribute ID, then the existing attribute is replaced by the new attribute.
	 *
	 * @param attributeId
	 * 				The ID of the new attribute as {@link RexsAttributeId}.
	 * @param matrixValues
	 * 				The value of the new attribute as {@link Double[][]}.
	 */
	public void addAttribute(RexsAttributeId attributeId, Double[][] matrixValues) {
		RexsAttribute attribute = createAndAddAttribute(attributeId);
		attribute.setDoubleMatrixValue(matrixValues);
	}

	/**
	 * Changes the numeric ID of the component.
	 * <p>
	 * ATTENTION: Make sure that the new ID has not already been assigned to another component.
	 *
	 * @param newId
	 * 				The new numeric ID of the component within the REXS model.
	 */
	protected void changeComponentId(Integer newId) {
		id = newId;
		rawComponent.setId(newId);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int compareTo(RexsComponent other) {
		return getId() - other.getId();
	}

	/**
	 * Deletes the attribute of the component for an attribute ID.
	 *
	 * @param attributeId
	 * 				The ID of the attribute as {@link RexsAttributeId}.
	 */
	public void deleteAttribute(RexsAttributeId attributeId) {
		if (hasAttribute(attributeId)) {
            RexsAttribute attribute = attributes.get(attributeId.getId());
			attributes.remove(attributeId.getId());
			rawComponent.getAttribute().remove(attribute.getRawAttribute());
		}
	}

	/**
	 * Returns whether the component is of a given list of types
	 *
	 * @param rexsComponentTypes
	 * 				an array containing the allowed types
	 *
	 * @return
	 * 				whether the component type is in the list of types
	 */
	public boolean isOfType(RexsComponentType... rexsComponentTypes) {
		return this.type.isOneOf(rexsComponentTypes);
	}
}
