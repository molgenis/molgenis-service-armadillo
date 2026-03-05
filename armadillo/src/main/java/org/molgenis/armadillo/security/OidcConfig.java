package org.molgenis.armadillo.security;

public record OidcConfig(String issuerUri, String clientId, String clientSecret) {}
