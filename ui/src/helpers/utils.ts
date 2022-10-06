import { ListOfObjectsWithStringKey } from "@/types/types";

export function stringIncludesOtherString(string: string, substring: string): boolean {
  return string.toLowerCase().includes(substring.toLowerCase());
}

export function toCapitalizedWords(name: string): string {
  const words = name.match(/[A-Za-z][a-z]*/g) || [];
  return words.map(capitalize).join(" ");
}

export function capitalize(word: string): string {
  return word.charAt(0).toUpperCase() + word.substring(1);
}

export function sortAlphabetically(listOfObjects: ListOfObjectsWithStringKey, key: string): ListOfObjectsWithStringKey {
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

export function getEventValue(event: Event): string {
  const target = event.target as HTMLInputElement;
  return target.value;
}