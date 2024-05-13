import {
  ListOfObjectsWithStringKey,
  ObjectWithStringKey,
  StringArray,
} from "@/types/types";

export function stringIncludesOtherString(
  completeString: string,
  substring: string
): boolean {
  return completeString.toLowerCase().includes(substring.toLowerCase());
}

export function toCapitalizedWords(name: string): string {
  const words = name.match(/[A-Za-z][a-z]*/g) || [];
  return words.map(capitalize).join(" ");
}

export function capitalize(word: string): string {
  return word.charAt(0).toUpperCase() + word.substring(1);
}

export function sortAlphabetically(
  listOfObjects: ListOfObjectsWithStringKey | StringArray,
  key?: string
): ListOfObjectsWithStringKey | string[] {
  return listOfObjects.sort((object1, object2) => {
    let obj1;
    let obj2;

    if (typeof object1 === "object" && key !== undefined) {
      obj1 = (object1 as ObjectWithStringKey)[key];
      obj2 = (object2 as ObjectWithStringKey)[key];
    } else {
      obj1 = object1;
      obj2 = object2;
    }

    if (obj1 < obj2) {
      return -1;
    }
    if (obj1 > obj2) {
      return 1;
    }
    return 0;
  });
}

export function shortenFileName(FileName: string): string {
  const max_length = 14;
  const extension_index = FileName.indexOf(".");
  // Check if the filename, before extension, exceeds the "max_length"
  if (FileName.substring(0, extension_index).length > max_length) {
    return (
      truncate(FileName, max_length, "{..}") +
      FileName.substring(extension_index, FileName.length)
    );
  } else {
    return FileName;
  }
}

export function getEventValue(event: Event): string {
  const target = event.target as HTMLInputElement;
  return target.value;
}

export function truncate(
  stringToCut: string,
  maxLength: number,
  truncationIndicator: string = ".."
) {
  return stringToCut.substring(0, maxLength) + truncationIndicator;
}

export function isInt(itemToCheck: number) {
  return itemToCheck % 1 === 0;
}

export function isIntArray(listOfItems: StringArray) {
  let itemIsIntArray = true;
  listOfItems.forEach((item) => {
    const numberToCheck = parseFloat(item);
    if (!isInt(numberToCheck)) {
      itemIsIntArray = false;
      return;
    }
  });
  return itemIsIntArray;
}

export function transformTable(table: { [key: string]: string }[]) {
  let transformed: { [key: string]: StringArray } = {};
  table.forEach((row) => {
    const keys = Object.keys(row);
    keys.forEach((key) => {
      if (key in transformed) {
        transformed[key].push(row[key]);
      } else {
        transformed[key] = [row[key]];
      }
    });
  });
  return transformed;
}

export function isDuplicate(key: string, list: StringArray) {
  let found = false;
  let isDuplicate = false;
  list.forEach((item) => {
    if (item === key) {
      if (!found) {
        found = true;
      } else {
        isDuplicate = true;
      }
    }
  });
  return isDuplicate;
}

export function sanitizeObject(
  objectToClean: ObjectWithStringKey
): ObjectWithStringKey {
  let sanitizedObject: ObjectWithStringKey = {};
  Object.keys(objectToClean).forEach((key: string) => {
    let sanitized: string | number | boolean | Object | Array<any>;
    const sanitizedKey: string = key.trim();
    const value = objectToClean[key];
    if (Array.isArray(value)) {
      sanitized = [];
      value.forEach((item) => {
        if (typeof item === "string") {
          (sanitized as Array<any>).push(item.trim());
        } else {
          (sanitized as Array<any>).push(item);
        }
      });
      sanitizedObject[sanitizedKey] = sanitized;
    } else if (typeof value === "string") {
      sanitizedObject[sanitizedKey] = (value as string).trim();
    } else {
      sanitizedObject[sanitizedKey] = value;
    }
  });
  return sanitizedObject;
}

export function isEmptyObject(obj: Object) {
  return Object.keys(obj).length === 0;
}

/**
 * Get a cloned version of the input.
 *
 * @param input:Object
 */
export function objectDeepCopy<T>(input: T): T {
  return JSON.parse(JSON.stringify(input));
}

export function isTableType(item: string): boolean {
  return item.endsWith(".parquet");
}

export function isNonTableType(item: string): boolean {
  return !isTableType(item);
}

export function isLinkFileType(item: string): boolean {
  return item.endsWith(".alf");
}

export function getRestructuredProject(
  projectContent: StringArray,
  projectId: string
): Record<string, StringArray> {
  let content: Record<string, StringArray> = {};
  projectContent.forEach((item) => {
    /** scrub the project folder from the name */
    const itemInProjectFolder = item.replace(`${projectId}/`, "");
    if (itemInProjectFolder.length && itemInProjectFolder[0] === ".") {
      return; /** if item starts with a . */
    }

    /** Check if it is in a subfolder */
    if (itemInProjectFolder.includes("/")) {
      const splittedItem = itemInProjectFolder.split("/");
      const folder = splittedItem[0];
      const folderItem = splittedItem[1];

      /** add to the content structure */
      if (content[folder]) {
        content[folder] = content[folder].concat(folderItem);
      } else {
        content[folder] = [folderItem];
        if (folderItem === "") {
          content[folder] = [];
        }
      }
    }
  });
  return content;
}

export function getTablesFromListOfFiles(
  listOfFiles: StringArray
): StringArray {
  return listOfFiles
    ? listOfFiles.filter((file: string) => file.endsWith(".parquet"))
    : [];
}

export function encodeUriComponent(component: string) {
  return component.replaceAll("/", "%2F").replaceAll("-", "%2D");
}

export function diskSpaceBelowThreshold(diskSpace: number): boolean {
  return diskSpace < 2147483648;
}

/**
 * Convert given bytes to 2 digits precision round exponent version string.
 * @param bytes number
 */
export function convertBytes(bytes: number): string {
  const units = ["bytes", "KB", "MB", "GB", "TB", "EB"];
  let unitIndex = 0;
  while (bytes >= 1024 && unitIndex < units.length - 1) {
    bytes /= 1024;
    unitIndex++;
  }

  return `${bytes.toFixed(2)} ${units[unitIndex]}`;
}
