package io.eventuate.javaclient.restclient;

import io.eventuate.javaclient.commonimpl.JSonMapper;
import org.apache.commons.io.IOUtils;
import org.everit.json.schema.Schema;
import org.everit.json.schema.ValidationException;
import org.everit.json.schema.loader.SchemaClient;
import org.everit.json.schema.loader.SchemaLoader;
import org.everit.json.schema.loader.internal.DefaultSchemaClient;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import static junit.framework.TestCase.assertNotNull;
import static junit.framework.TestCase.fail;

public class AbstractApiComplianceTest {

  protected void shouldConformTo(Object request, String schemaFile) throws FileNotFoundException {
    String json = JSonMapper.toJson(request);
    shouldConformTo(request, schemaFile, json);
  }

  protected void shouldConformTo(String json, String schemaFile) throws FileNotFoundException {
    shouldConformTo(json, schemaFile, json);
  }

  private void shouldConformTo(Object request, String schemaFile, String json) throws FileNotFoundException {
    InputStream inputStream =getClass().getResourceAsStream("/io/eventuate/apispec/v1/schemas/" + schemaFile);
    assertNotNull(schemaFile, inputStream);
    JSONObject rawSchema = new JSONObject(new JSONTokener(inputStream));
    Schema schema = SchemaLoader.load(rawSchema, new MySchemaClient());
    try {
      schema.validate(new JSONObject(json)); // throws a ValidationException if this object is invalid
    } catch (ValidationException e) {
      StringBuffer sb = new StringBuffer();
      sb.append(json).append(" did not conform to ").append(schemaFile).append('\n');
      sb.append(e.getMessage());
      for (ValidationException ee : e.getCausingExceptions())
        sb.append("\n").append(ee.getMessage());
      fail(sb.toString());
    }
  }

  protected String getJsonResourceConformingTo(String resource, String schemaFile) throws IOException {
    String json = getJsonResource(resource);
    shouldConformTo(json, schemaFile);
    return json;
  }

  private String getJsonResource(String resource) throws IOException {
    return IOUtils.toString(getClass().getResourceAsStream(resource));
  }

  class MySchemaClient implements SchemaClient {
    private DefaultSchemaClient defaultClient = new DefaultSchemaClient();

    @Override
    public InputStream get(String url) {
      String prefix = "https://api.eventuate.io/schemas/v1/";
      if (url.startsWith(prefix)) {
        try {
          return new FileInputStream("/Users/cer/src/shopcookeat/refactored-backend/eventuate-api-spec/v1/schemas/" + url.substring(prefix.length()));
        } catch (FileNotFoundException e) {
          throw new RuntimeException(e);
        }
      } else
        return defaultClient.get(url);
    }
  }
}
