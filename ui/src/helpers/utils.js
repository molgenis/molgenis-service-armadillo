export function stringIncludesOtherString(string, substring) {
  return string.toLowerCase().includes(substring.toLowerCase());
}

export function toCapitalizedWords(name) {
    const words = name.match(/[A-Za-z][a-z]*/g) || [];
    return words.map(capitalize).join(" ");
  }

export function capitalize(word) {
    return word.charAt(0).toUpperCase() + word.substring(1);
}