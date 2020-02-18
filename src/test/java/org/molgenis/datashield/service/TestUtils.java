package org.molgenis.datashield.service;

import com.google.gson.Gson;
import org.molgenis.api.metadata.model.EntityType;

import java.io.InputStreamReader;

import static java.nio.charset.StandardCharsets.UTF_8;

public class TestUtils
{
	public static EntityType getEntityType(String jsonFileName)
	{
		return new Gson().fromJson(new InputStreamReader(TestUtils.class.getResourceAsStream(jsonFileName), UTF_8), EntityType.class);
	}
}
