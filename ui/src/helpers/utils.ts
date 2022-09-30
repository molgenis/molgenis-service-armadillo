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

export function sortAlphabetically(listOfObjects, key) {
  return listOfObjects.sort((object1, object2) => {
    if (object1[key] < object2[key]) {
      return -1;
    }
    if (object1[key] > object2[key]) {
      return 1;
    }
    return 0;
  });
}
