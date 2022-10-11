import {stringIncludesOtherString, toCapitalizedWords, capitalize, sortAlphabetically} from "@/helpers/utils";

describe("utils", () => {
  describe("stringIncludesOtherString", () => {
    it("should return true for string and substring", () => {
        const actual = stringIncludesOtherString('string1', 'ring');
        expect(actual).toBe(true);
    });

    it("should return false for string and substring that don't match", () => {
        const actual = stringIncludesOtherString('string1', 'somethingelse');
        expect(actual).toBe(false);
    });
  });

  describe("toCapitalizedWords", () => {
    it("should return capitalised words from a camelcased string", () => {
        const actual = toCapitalizedWords('camelsAreAwesome');
        expect(actual).toBe('Camels Are Awesome');
    });
  });

  describe("capitalize", () => {
    it("should return capitalised version of word", () => {
        const actual = capitalize('camelsAreAwesome');
        expect(actual).toBe('CamelsAreAwesome');
    });
  });

  describe("sortAlphabetically", () => {
    it("should return capitalised version of word", () => {
        const actual = sortAlphabetically([
            {"animal": "camel", "humps": 2, "horns": 0},
            {"animal": "dromedary", "humps": 1, "horns": 0},
            {"animal": "unicorn", "humps": 0, "horns": 1},
            {"animal": "brahman", "humps": 1, "horns": 2},
            {"animal": "zebu", "humps": 1, "horns": 2},
            {"animal": "rhinoceros", "humps": 0, "horns": 2},
            {"animal": "cow", "humps": 0, "horns": 2},
            {"animal": "horse", "humps": 0, "horns": 0},
        ], "animal");
        const sorted = [
            {"animal": "brahman", "humps": 1, "horns": 2},
            {"animal": "camel", "humps": 2, "horns": 0},
            {"animal": "cow", "humps": 0, "horns": 2},
            {"animal": "dromedary", "humps": 1, "horns": 0},
            {"animal": "horse", "humps": 0, "horns": 0},
            {"animal": "rhinoceros", "humps": 0, "horns": 2},
            {"animal": "unicorn", "humps": 0, "horns": 1},
            {"animal": "zebu", "humps": 1, "horns": 2},
        ]
        expect(actual).toMatchObject(sorted);
    });
  });
});
