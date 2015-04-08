package org.lqnotify.pub;

import org.tiogasolutions.dev.common.json.JsonTranslator;
import org.tiogasolutions.dev.jackson.TiogaJacksonTranslator;
import org.testng.annotations.*;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;

@Test
public class ExceptionInfoTest {

  private JsonTranslator translator;

  @BeforeClass
  public void beforeClass() throws Exception {
    translator = new TiogaJacksonTranslator();
  }

  public void testTranslateExceptionInfo() throws Exception {
    try {
      throw new TestExceptionA("You did something really bad");

    } catch (TestExceptionA exceptionA) {
      TestExceptionB exceptionB = new TestExceptionB(exceptionA);
      ExceptionInfo oldValue = new ExceptionInfo(exceptionB);
      ExceptionInfo oldCause = oldValue.getCause();

      String json = translator.toJson(oldValue);
      assertEquals(json, EXPECTED_JSON);

      ExceptionInfo newValue = translator.fromJson(ExceptionInfo.class, json);
      ExceptionInfo newCause = newValue.getCause();

      assertEquals(newValue.getMessage(), oldValue.getMessage());

      assertEquals(newValue.getStackTrace(), oldValue.getStackTrace());
      assertNotNull(newCause);

      assertEquals(newCause.getMessage(), oldCause.getMessage());
      assertEquals(newCause.getStackTrace(), oldCause.getStackTrace());
      assertNull(newCause.getCause());
    }
  }

  private static final String EXPECTED_JSON = "{\n" +
    "  \"exceptionType\" : \"org.lqnotify.pub.TestExceptionB\",\n" +
    "  \"message\" : \"org.lqnotify.pub.TestExceptionA: You did something really bad\",\n" +
    "  \"stackTrace\" : \"org.lqnotify.pub.TestExceptionB: org.lqnotify.pub.TestExceptionA: You did something really bad\\n\\tat org.lqnotify.pub.TestExceptionB.firstMethod(TestExceptionB.java:133)\\n\\tat org.lqnotify.pub.TestExceptionB.secondMethod(TestExceptionB.java:344)\\n\\tat org.lqnotify.pub.TestExceptionB.thirdMethod(TestExceptionB.java:352)\\nCaused by: org.lqnotify.pub.TestExceptionA: You did something really bad\\n\\tat org.lqnotify.pub.TestExceptionA.topMethod(TestExceptionA.java:13)\\n\\tat org.lqnotify.pub.TestExceptionA.middleMethod(TestExceptionA.java:34)\\n\\tat org.lqnotify.pub.TestExceptionA.bottomMethod(TestExceptionA.java:32)\\n\",\n" +
    "  \"cause\" : {\n" +
    "    \"exceptionType\" : \"org.lqnotify.pub.TestExceptionA\",\n" +
    "    \"message\" : \"You did something really bad\",\n" +
    "    \"stackTrace\" : \"org.lqnotify.pub.TestExceptionA: You did something really bad\\n\\tat org.lqnotify.pub.TestExceptionA.topMethod(TestExceptionA.java:13)\\n\\tat org.lqnotify.pub.TestExceptionA.middleMethod(TestExceptionA.java:34)\\n\\tat org.lqnotify.pub.TestExceptionA.bottomMethod(TestExceptionA.java:32)\\n\",\n" +
    "    \"cause\" : null\n" +
    "  }\n" +
    "}";
}