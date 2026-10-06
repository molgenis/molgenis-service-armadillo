export function getDsDescriptions() {
  return {
    "nfilter.tab":
      "The minimum non-zero cell count allowed in any cell if a contingency table is to be returned. This applies to one dimensional and two dimensional tables of counts tabulated across one or two factors and to tables of a mean of a quantitative variable tabulated across a factor. Default usually set to 3 but a value of 1 (no limit) may be necessary, particularly if low cell counts are highly probable such as when working with rare diseases. Five is also a justifiable choice to replicate the most common threshold rule imposed by data releasers worldwide; but it should be recognised that many census providers are moving to ten.",
    "nfilter.subset":
      "The minimum non-zero count of observational units (typically individuals) in a subset. Typically defaulted to 3.",
    "nfilter.glm":
      "The maximum length of a string argument if that argument is to be subject to testing of its length. Default value = 80. The aim of this nfilter is to make it difficult for hackers to find a way to embed malicious code in a valid string argument that is actively interpreted.",
    "nfilter.string":
      "The maximum length of a string argument if that argument is to be subject to testing of its length. Default value = 80. The aim of this nfilter is to make it difficult for hackers to find a way to embed malicious code in a valid string argument that is actively interpreted.",
    "nfilter.stringShort": "Same as above but set to 20 characters",
    "nfilter.kNN":
      "The minimum value allowed for k on the k-nearest neighbours method which is used mainly for some of the graphical functions. Default value = 3.",
    "nfilter.levels.density":
      "The maximum proportion of unique levels of a categorical variable with respect to the number of that variables that is regarded as non-disclosive. For example, if the resulting contains 1000 levels, and were derived from 4000 rows what would be a proportion of 0.25 (25%) so would be regarded as being non-disclosive. Default value is 0.33.",
    "nfilter.levels.max":
      "The maximum number of unique levels of a categorical variable that is regarded as non-disclosive. Default value is 40.",
    "nfilter.noise":
      'The minimum level of noise that can be added to a server-side vector. The "noisy" vector can then be returned to the client. This value specifies the variance of the added noise. If nfilter.noise is set to 0.25 (its default value) then noise following a distribution (usually Gaussian) with zero mean and variance equal to the 25% of the true variance of the vector of interest is added to each individual value of that vector.',
    "datashield.privacyControlLevel":
      "Permits server administrators to run servers with a predefined subset of the standard methods available. <a href='https://wiki.datashield.org/en/opmanag/disclosure-control' target='_blank'>More information.</a>",
    "datashield.seed":
      "Primes the pseudorandom number generator in a data source.",
  };
}
