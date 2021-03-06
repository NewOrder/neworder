/**
    This file is part of NewOrder.

    NewOrder is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    NewOrder is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with Foobar.  If not, see <http://www.gnu.org/licenses/>.
*/

package no.neworder.util;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class TestCaseMapperTest {

	@Test
	public void should_parse_file_path_into_package() {
		String fileName1 = "src/main/java/no/neworder/Example.java";
		String fileName2 = "src/test/java/no/neworder/Examplee.java";
		
		TestCaseMapper mapper = new TestCaseMapper(
				new String[]{"src/main/java", "src/test/java"});
		
		assertThat(mapper.parseFilePathToPackage(fileName1),
				is(equalTo("no.neworder.Example")));
		
		assertThat(mapper.parseFilePathToPackage(fileName2),
				is(equalTo("no.neworder.Examplee")));
	}
	
	@Test
	public void should_return_name_of_test_case_given_class_name() {
		String className = "Example";
		
		assertThat(TestCaseMapper.toTestCase(className),
			is(equalTo("ExampleTest")));
	}
	
	@Test
	public void should_return_name_of_test_case_given_test_case_name() {
		String className = "ExampleTest";
		
		assertThat(TestCaseMapper.toTestCase(className),
			is(equalTo("ExampleTest")));
	}
	
	@Test
	public void should_remove_path_and_append_test_suffix() {
		TestCaseMapper mapper = new TestCaseMapper(
				new String[]{"src/main/java", "src/test/java"});
		
		String testCaseName =
			mapper.prepareTestCaseName("src/main/java/no/neworder/Example.java");
		
		assertThat(testCaseName, is(equalTo("no.neworder.ExampleTest")));
	}
}
