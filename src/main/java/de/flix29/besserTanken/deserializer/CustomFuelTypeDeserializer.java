package de.flix29.besserTanken.deserializer;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import de.flix29.besserTanken.model.kraftstoffbilliger.FuelType;

import java.lang.reflect.Type;

public class CustomFuelTypeDeserializer extends CustomDeserializerUtils implements JsonDeserializer<FuelType> {
    @Override
    public FuelType deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
        int id = jsonElement.getAsJsonObject().get("id").getAsInt();
        return FuelType.fromId(id);
    }
}
