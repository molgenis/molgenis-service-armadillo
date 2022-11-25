import {
  stringIncludesOtherString,
  toCapitalizedWords,
  capitalize,
  sortAlphabetically,
  truncate,
  isInt,
  isIntArray,
  transformTable,
} from "@/helpers/utils";

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
    });
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
});