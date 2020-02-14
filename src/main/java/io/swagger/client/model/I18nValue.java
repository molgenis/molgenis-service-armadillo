/*
 * Metadata API
 * RESTful API to create/read/update/delete metadata.
 *
 * OpenAPI spec version: 3.0.0-SNAPSHOT
 * 
 *
 * NOTE: This class is auto generated by the swagger code generator program.
 * https://github.com/swagger-api/swagger-codegen.git
 * Do not edit the class manually.
 */

package io.swagger.client.model;

import com.google.gson.annotations.SerializedName;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Objects;

/**
 * I18nValue
 */

@javax.annotation.Generated(value = "io.swagger.codegen.v3.generators.java.JavaClientCodegen", date = "2020-02-14T15:06:09.233Z[GMT]")
public class I18nValue {
  @SerializedName("defaultValue")
  private String defaultValue = null;

  @SerializedName("translations")
  private Object translations = null;

  public I18nValue defaultValue(String defaultValue) {
    this.defaultValue = defaultValue;
    return this;
  }

   /**
   * Get defaultValue
   * @return defaultValue
  **/
  @Schema(description = "")
  public String getDefaultValue() {
    return defaultValue;
  }

  public void setDefaultValue(String defaultValue) {
    this.defaultValue = defaultValue;
  }

  public I18nValue translations(Object translations) {
    this.translations = translations;
    return this;
  }

   /**
   * Get translations
   * @return translations
  **/
  @Schema(description = "")
  public Object getTranslations() {
    return translations;
  }

  public void setTranslations(Object translations) {
    this.translations = translations;
  }


  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    I18nValue i18nValue = (I18nValue) o;
    return Objects.equals(this.defaultValue, i18nValue.defaultValue) &&
        Objects.equals(this.translations, i18nValue.translations);
  }

  @Override
  public int hashCode() {
    return Objects.hash(defaultValue, translations);
  }


  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class I18nValue {\n");

    sb.append("    defaultValue: ").append(toIndentedString(defaultValue)).append("\n");
    sb.append("    translations: ").append(toIndentedString(translations)).append("\n");
    sb.append("}");
    return sb.toString();
  }

  /**
   * Convert the given object to string with each line indented by 4 spaces
   * (except the first line).
   */
  private String toIndentedString(Object o) {
    if (o == null) {
      return "null";
    }
    return o.toString().replace("\n", "\n    ");
  }

}
