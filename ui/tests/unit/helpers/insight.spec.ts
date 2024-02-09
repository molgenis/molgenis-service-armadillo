import { matchedLineIndices } from "@/helpers/insight";

describe('increment', () => {
    test('single line', () => {
        expect(matchedLineIndices(["a"], 'a')).toStrictEqual([0])
    });

    test('two equal lines', () => {
        expect(matchedLineIndices(["a", "a"], 'a')).toStrictEqual([0, 1])
    });

    test('two different lines found 1', () => {
        expect(matchedLineIndices(["a", "B"], 'a')).toStrictEqual([0])
    });

    test('two different lines non found', () => {
        expect(matchedLineIndices(["a", "B"], 'c')).toStrictEqual([])
        expect(matchedLineIndices(["ab", "AC"], 'D')).toStrictEqual([])
    });

    test('three different lines 2 found', () => {
        expect(matchedLineIndices(["ad", "Bd", "C"], 'd')).toStrictEqual([0, 1])
    });

})