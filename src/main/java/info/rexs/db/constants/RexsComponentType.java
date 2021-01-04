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
package info.rexs.db.constants;

import java.util.HashMap;
import java.util.Map;

import info.rexs.db.constants.standard.RexsStandardComponentTypes;
import lombok.EqualsAndHashCode;

/**
 * This class represents the type of a REXS component.
 * <p>
 * It contains constants for all component types of official REXS versions (1.0, 1.1, 1.2, 1.3).
 * <p>
 * Since REXS is freely expandable, you can also add your own component types using the {@link #create(String)} method.
 *
 * @author FVA GmbH
 */
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class RexsComponentType implements RexsStandardComponentTypes {

	/** An internal index with all created component types (REXS standard and own) for quick access. */
	private static Map<String, RexsComponentType> allComponentTypes = new HashMap<>();

	/** The actual ID of the component type as a {@link String}. */
	@EqualsAndHashCode.Include
	private final String id;

	private RexsComponentType(String id) {
		if (id == null || id.isEmpty())
			throw new IllegalArgumentException("id cannot be empty");
		this.id = id;
	}

	/**
	 * @return
	 * 				The actual ID of the component type as a {@link String}.
	 */
	public String getId() {
		return id;
	}

	/**
	 * TODO Document me!
	 *
	 * @param checkComponentTypes
	 * 				TODO Document me!
	 *
	 * @return
	 * 				TODO Document me!
	 */
	public boolean isOneOf(RexsComponentType ... checkComponentTypes)
	{
		if (checkComponentTypes == null)
			return false;

		for (RexsComponentType checkComponentType : checkComponentTypes)
		{
			if (this.equals(checkComponentType))
				return true;
		}

		return false;
	}

	/**
	 * Creates a new component type and adds it to the internal index.
	 *
	 * @param id
	 * 				The actual ID of the component type as a {@link String}.
	 *
	 * @return
	 * 				The newly created component type as {@link RexsComponentType}.
	 */
	public static RexsComponentType create(String id) {
		RexsComponentType componentType = new RexsComponentType(id);
		allComponentTypes.put(id, componentType);
		return componentType;
	}

	/**
	 * Returns the component type for a textual ID from the internally stored index of all component types.
	 *
	 * @param id
	 * 				The actual ID of the component type to be found as a {@link String}
	 *
	 * @return
	 * 				The found component type as {@link RexsComponentType}, or {@code null} if the ID could not be found.
	 */
	public static RexsComponentType findById(String id) {
		if (id == null)
			return null;
		RexsStandardComponentTypes.init();
		return allComponentTypes.getOrDefault(id, null);
	}
}
