import {
  stringIncludesOtherString,
  toCapitalizedWords,
  capitalize,
  sortAlphabetically,
  truncate,
  isInt,
  isIntArray,
  transformTable,
  isDuplicate,
  sanitizeObject,
  isEmptyObject,
  shortenFileName,
  isTableType,
  isNonTableType,
  getRestructuredProject,
  getTablesFromListOfFiles,
  isLinkFileType,
  encodeUriComponent,
  convertBytes,
  diskSpaceBelowThreshold,
  isEmpty,
  convertStringToBytes,
  isDate
} from "@/helpers/utils";
import { StringObject } from "@/types/types";

describe("utils", () => {
  describe("stringIncludesOtherString", () => {
    it("should return true for string and substring", () => {
      const actual = stringIncludesOtherString("string1", "ring");
      expect(actual).toBe(true);
    });

    it("should return false for string and substring that don't match", () => {
      const actual = stringIncludesOtherString("string1", "somethingelse");
      expect(actual).toBe(false);
    });
  });

  describe("toCapitalizedWords", () => {
    it("should return capitalised words from a camelcased string", () => {
      const actual = toCapitalizedWords("camelsAreAwesome");
      expect(actual).toBe("Camels Are Awesome");
    });
  });

  describe("capitalize", () => {
    it("should return capitalised version of word", () => {
      const actual = capitalize("camelsAreAwesome");
      expect(actual).toBe("CamelsAreAwesome");
    });
  });

  describe("sortAlphabetically", () => {
    it("should return capitalised version of word", () => {
      const actual = sortAlphabetically(
        [
          { animal: "camel", humps: 2, horns: 0 },
          { animal: "dromedary", humps: 1, horns: 0 },
          { animal: "unicorn", humps: 0, horns: 1 },
          { animal: "brahman", humps: 1, horns: 2 },
          { animal: "zebu", humps: 1, horns: 2 },
          { animal: "rhinoceros", humps: 0, horns: 2 },
          { animal: "cow", humps: 0, horns: 2 },
          { animal: "horse", humps: 0, horns: 0 },
        ],
        "animal"
      );
      const sorted = [
        { animal: "brahman", humps: 1, horns: 2 },
        { animal: "camel", humps: 2, horns: 0 },
        { animal: "cow", humps: 0, horns: 2 },
        { animal: "dromedary", humps: 1, horns: 0 },
        { animal: "horse", humps: 0, horns: 0 },
        { animal: "rhinoceros", humps: 0, horns: 2 },
        { animal: "unicorn", humps: 0, horns: 1 },
        { animal: "zebu", humps: 1, horns: 2 },
      ];
      expect(actual).toMatchObject(sorted);
    });
  });

  describe("shortenFileName", () => {
    it("should shorten string longer than maximum allowed", () => {
      const actual = shortenFileName("averylongstringthatisoverthemaximumallowed.parquet")
      expect(actual).toBe("averylongstrin{..}.parquet")
    });
    it("should not shorten string if shorter than maximum allowed", () => {
      const actual = shortenFileName("somefile.parquet")
      expect(actual).toBe("somefile.parquet")
    });
    it("should not shorten string if no extension is supplied", () => {
      const actual = shortenFileName("averlongstringthatisovermaximumbutnoextension")
      expect(actual).toBe("averlongstringthatisovermaximumbutnoextension")
    });
    it("should function even if the extension is gzipped tsv", () => {
      const actual = shortenFileName("averylongstringthatisoverthemaximumallowed.tsv.gz")
      expect(actual).toBe("averylongstrin{..}.tsv.gz")
    })
  });

  describe("truncate", () => {
    it("should truncate value longer than maxLength", () => {
      const actual = truncate("averylongstring", 3);
      expect(actual).toBe("ave..");
    });
    it("should not truncate value shorter than maxLength", () => {
      const actual = truncate("string", 10);
      expect(actual).toBe("string..");
    });
  });

  describe("isInt", () => {
    it("should return true for int", () => {
      const actual = isInt(3);
      expect(actual).toBe(true);
    });
    it("should return true for whole float", () => {
      const actual = isInt(3.0);
      expect(actual).toBe(true);
    });
    it("should return false for actual float", () => {
      const actual = isInt(3.01);
      expect(actual).toBe(false);
    })
  });

  describe("isIntArray", () => {
    it("should return false for string array with floats", () => {
      const actual = isIntArray(["1.0", "1.11", "2.8"]);
      expect(actual).toBe(false);
    });
    it("should return true for string array with whole floats", () => {
      const actual = isIntArray(["1.0", "1.0", "2.0"]);
      expect(actual).toBe(true);
    });
    it("should return false for string array with ints", () => {
      const actual = isIntArray(["1", "1", "2"]);
      expect(actual).toBe(true);
    });
    it("should return false for string array with dates", () => {
      const actual = isIntArray(["2024-12-05T12:27:49.107+01:00", "2024-12-05T12:27:49.107+01:00", "2024-12-05T12:27:49.107+01:00"]);
      expect(actual).toBe(false);
    });
    it("should return false for string array with strings", () => {
      const actual = isIntArray(["test1", "test2", "test3"]);
      expect(actual).toBe(false);
    });
  });

  describe("transformTable", () => {
    it("should transform table", () => {
      const actual = transformTable([
        { col1: "val1", col2: "val2", col3: "val3" },
        { col1: "vala", col2: "valb", col3: "valc" },
      ]);
      expect(actual).toEqual({
        col1: ["val1", "vala"],
        col2: ["val2", "valb"],
        col3: ["val3", "valc"],
      });
    });
  });

  describe("isDuplicate", () => {
    const testData = ["val1", "val2", "val3", "val4", "val1"];
    it("should return true if value duplicate", () => {
      expect(isDuplicate("val1", testData)).toBe(true);
    });
    it("should return false if value not present", () => {
      expect(isDuplicate("val0", testData)).toBe(false);
    });
    it("should return false if value present once", () => {
      expect(isDuplicate("val2", testData)).toBe(false);
    });
  });

  describe("sanitizeObject", () => {
    const testData = {
      key1: "simple value",
      key2: " dirty value",
      key3: "another dirty value ",
      key4: "     a very dirty value      ",
      " dirty key ": "clean value",
      "another dirty key ": "another clean value",
      key5: [
        "i am clean",
        "i am clean as well",
        " i am dirty",
        "i am dirty too ",
        " i am very dirty ",
      ],
      " dirty ": [" dirty", " very dirty ", "clean"],
      "\tboolean": false,
    };
    const output: StringObject = sanitizeObject(testData) as StringObject;
    it("should trim spaces at beginning and end of string values", () => {
      expect(output["key1"]).toBe("simple value");
      expect(output["key2"]).toBe("dirty value");
      expect(output["key3"]).toBe("another dirty value");
    });
    it("should trim spaces at beginning and end of keys", () => {
      expect(Object.keys(output)).toContain("dirty key");
      expect(Object.keys(output)).toContain("another dirty key");
      expect(Object.keys(output)).toContain("dirty");
      expect(Object.keys(output)).not.toContain(" dirty key");
      expect(Object.keys(output)).not.toContain("another dirty key ");
      expect(Object.keys(output)).not.toContain(" dirty ");
    });
    it("should trim spaces at beginning and end of all strings in array values", () => {
      expect(output["key5"]).toEqual([
        "i am clean",
        "i am clean as well",
        "i am dirty",
        "i am dirty too",
        "i am very dirty",
      ]);
      expect(output["dirty"]).toEqual(["dirty", "very dirty", "clean"]);
    });
    it("should not do anything to booleans", () => {
      expect(output["boolean"]).toBe(false);
    });
  });
  describe("isEmptyObject", () => {
    it("should return true if object is empty", () => {
      const actual = isEmptyObject({});
      expect(actual).toBe(true);
    });
    it("should return false if object is not empty", () => {
      const actual = isEmptyObject({ key: "value" });
      expect(actual).toBe(false);
    });
  });
  describe("isTableType", () => {
    it("should return true if item has parquet extension", () => {
      const actual = isTableType("test.parquet");
      expect(actual).toBe(true);
    });
    it("should return false if item doesnt have parquet extension", () => {
      const actual = isTableType("test.somethingelse");
      expect(actual).toBe(false);
    });
  });

  describe("isLinkFileType", () => {
    it("should return true if item has parquet extension", () => {
      const actual = isLinkFileType("test.alf");
      expect(actual).toBe(true);
    });
    it("should return false if item doesnt have parquet extension", () => {
      const actual = isLinkFileType("test.somethingelse");
      expect(actual).toBe(false);
    });
  }); 

  describe("isNonTableType", () => {
    it("should return false if item has parquet extension", () => {
      const actual = isNonTableType("test.parquet");
      expect(actual).toBe(false);
    });
    it("should return true if item doesnt have parquet extension", () => {
      const actual = isNonTableType("test.somethingelse");
      expect(actual).toBe(true);
    });
  });
  describe("getRestructuredProject", () => {
    it("should return restructured project object", ()=> {
     const testProject = [
       "lifecycle/.DS_Store",
       "lifecycle/core/yearlyrep.parquet",
       "lifecycle/core/trimesterrep.parquet",
       "lifecycle/core/monthlyrep.parquet",
       "lifecycle/core/nonrep.parquet",
       "lifecycle/outcome/yearlyrep.parquet",
       "lifecycle/outcome/nonrep.parquet",
       "lifecycle/survival/veteran.parquet"
     ];
     const projectId = "lifecycle";
     const actual = getRestructuredProject(testProject, projectId);
     const expected = {
       "core": ["yearlyrep.parquet", "trimesterrep.parquet", "monthlyrep.parquet", "nonrep.parquet"],
       "outcome": ["yearlyrep.parquet", "nonrep.parquet"],
       "survival": ["veteran.parquet"]
     }
     expect(actual).toEqual(expected);
    })
   });

   describe("getTablesFromListOfFiles", () => {
    it("should return a list of only parquetfiles from the supplied array with filenames", () => {
      const actual = getTablesFromListOfFiles(["aap.parquet", "test.csv", "test.parquet", "test.xlsx"]);
      expect(actual).toEqual(["aap.parquet", "test.parquet"]);
    });
  });

  describe("encodeUriComponent", () => {
    it("should encode / and -", () => {
      const actual = encodeUriComponent("project-id/folder/file-version1.parquet");
      expect(actual).toEqual("project%2Did%2Ffolder%2Ffile%2Dversion1.parquet");
    });
  });

  describe("convertBytes", () => {
    it("bytes", () => {
      const actual = convertBytes(999)
      expect(actual).toEqual("999.00 bytes")
    })
    it("KB", () => {
      const actual = convertBytes(99999)
      expect(actual).toEqual("97.66 KB")
    })
    it("MB", () => {
      const actual = convertBytes(9999999)
      expect(actual).toEqual("9.54 MB")
    })
    it("GB", () => {
      const actual = convertBytes(9999999999)
      expect(actual).toEqual("9.31 GB")
    })
    it("TB", () => {
      const actual = convertBytes(9999999999999)
      expect(actual).toEqual("9.09 TB")
    })
    it("EB", () => {
      const actual = convertBytes(9999999999999999)
      expect(actual).toEqual("8.88 EB")
    })
  });


  describe("diskSpaceBelowThreshold", () => {
    it("Return true", () => {
      const actual = diskSpaceBelowThreshold(214748364);
      expect(actual).toEqual(true);
    });
    it("Return false", () => {
      const actual = diskSpaceBelowThreshold(9214748364);
      expect(actual).toEqual(false);
    });

    it("Return false", () => {
      const actual = diskSpaceBelowThreshold(NaN);
      expect(actual).toEqual(false);
    });
  });

  describe("isEmpty", () => {
    it("Returns true when undefined", () => {
      const actual = isEmpty(undefined);
      expect(actual).toEqual(true);
    });

    it("Returns true when null", () => {
      const actual = isEmpty(null);
      expect(actual).toEqual(true);
    });

    it("Returns true when empty string", () => {
      const actual = isEmpty('');
      expect(actual).toEqual(true);
    });

    it("Returns true when empty array", () => {
      const actual = isEmpty([]);
      expect(actual).toEqual(true);
    });

    it("Returns true when empty object", () => {
      const actual = isEmpty({});
      expect(actual).toEqual(true);
    });

    it("Returns false when array with element", () => {
      const actual = isEmpty(['element']);
      expect(actual).toEqual(false);
    });

    it("Returns false when object with element", () => {
      const actual = isEmpty({"el": undefined});
      expect(actual).toEqual(false);
    });

    it("Returns false when string with content", () => {
      const actual = isEmpty("el");
      expect(actual).toEqual(false);
    });

    it("Returns false when number", () => {
      const actual = isEmpty(0);
      expect(actual).toEqual(false);
    });

    it("Returns true when NaN", () => {
      const actual = isEmpty(NaN);
      expect(actual).toEqual(true);
    });
  });

  describe('convertStringToBytes', () => {
    it('should convert KB to bytes correctly', () => {
      expect(convertStringToBytes("745.81 KB")).toEqual(763709.44);
      expect(convertStringToBytes("2.10 KB")).toEqual(2150.4);
    });
  
    it('should convert MB to bytes correctly', () => {
      expect(convertStringToBytes("844.53 MB")).toEqual(885553889.28);
      expect(convertStringToBytes("23.34 MB")).toEqual(24473763.84);
    });
  
    it('should convert GB to bytes correctly', () => {
      expect(convertStringToBytes("1.38 GB")).toEqual(1481763717.12);
      expect(convertStringToBytes("0.5 GB")).toEqual(536870912);
    });
  
    it('should convert TB to bytes correctly', () => {
      expect(convertStringToBytes("2.10 TB")).toEqual(2308974418329.6);
    });
  
    it('should convert EB to bytes correctly', () => {
      expect(convertStringToBytes("1.38 EB")).toEqual(1553741871442821);
    });
  
    it('should convert bytes to bytes correctly', () => {
      expect(convertStringToBytes("133.00 bytes")).toBe(133);
    });
  
    it('should handle a missing unit and default to bytes', () => {
      expect(convertStringToBytes("745.81")).toBeCloseTo(745.81, 1);
      expect(convertStringToBytes("1000")).toBe(1000);
    });
  
    it('should throw an error for invalid input format', () => {
      expect(() => convertStringToBytes("invalid input")).toThrow("Invalid size format");
      expect(() => convertStringToBytes("123.45 XY")).toThrow("Invalid size format");
    });
  });

  describe('isDate', () => {
  // Valid ISO 8601 date strings
  it('should return true for valid ISO 8601 date strings', () => {
    expect(isDate('2023-01-31T15:45:00Z')).toBe(true); // UTC with Z
    expect(isDate('2023-01-31T15:45:00+02:00')).toBe(true); // With timezone offset
    expect(isDate('2023-01-31T15:45:00-05:00')).toBe(true); // With negative timezone offset
    expect(isDate('2023-01-31T15:45:00.123Z')).toBe(true); // With milliseconds and UTC
    expect(isDate('2023-01-31T15:45:00.123+01:00')).toBe(true); // With milliseconds and timezone offset
  });

  // Empty strings and non-date inputs
  it('should return false for empty strings or non-date values', () => {
    expect(isDate('')).toBe(false); // Empty string
    expect(isDate('Hello, world!')).toBe(false); // Random text
    expect(isDate('12345')).toBe(false); // Random number as string
  });
});
});

  