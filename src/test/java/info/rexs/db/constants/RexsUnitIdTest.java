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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import org.junit.Test;

public class RexsUnitIdTest {

	@Test
	public void create_givenNullThrowsIllegalArgumentException() throws Exception {
		assertThatIllegalArgumentException()
			.isThrownBy(() -> {
				RexsUnitId.create(null);
			})
			.withMessage("id cannot be empty");
	}

	@Test
	public void create_givenEmptyIdThrowsIllegalArgumentException() throws Exception {
		assertThatExceptionOfType(IllegalArgumentException.class)
			.isThrownBy(() -> {
				RexsUnitId.create("");
			})
			.withMessage("id cannot be empty");
	}

	@Test
	public void create_newUnitIdHasId() throws Exception {
		RexsUnitId newUnitId = RexsUnitId.create("foo");
		assertThat(newUnitId.getId()).isEqualTo("foo");
	}

	@Test
	public void findById_givenNullReturnsNull() {
		assertThat(RexsUnitId.findById(null)).isNull();
	}

	@Test
	public void findById_givenUnknownIdReturnsNull() {
		assertThat(RexsUnitId.findById("foo_bar")).isNull();
	}

	@Test
	public void findById_returnsRexsStandardUnitId() throws Exception {
		RexsUnitId unitId = RexsUnitId.findById(RexsUnitId.hertz.getId());
		assertThat(unitId).isNotNull();
		assertThat(unitId.getId()).isEqualTo(RexsUnitId.hertz.getId());
	}

	@Test
	public void findById_returnsNewlyCreatedUnitId() throws Exception {
		RexsUnitId.create("bar");
		RexsUnitId newUnitId = RexsUnitId.findById("bar");
		assertThat(newUnitId).isNotNull();
		assertThat(newUnitId.getId()).isEqualTo("bar");
	}
}
