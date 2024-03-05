import { matchedLineIndices, auditJsonLinesToLines } from "@/helpers/insight";

describe('Matching lines on search string', () => {
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

describe('Prepare audit file', () => {
    describe('one base line', () => {
        const baseLine = {
            "timestamp":"2024-02-29T07:18:26.494967Z",
            "principal":"admin",
            "type":"FILE_DETAILS",
            "data":{
                "FILE_ID":"LOG_FILE",
                "sessionId":"4BEE1562A3E6F797ACA5CA7E353EC2CC",
                "roles":["ROLE_SU"]
            }
        }

        const processed = auditJsonLinesToLines([JSON.stringify(baseLine)])

        expect(processed.length).toBe(1)

        const lines = processed[0].split('\n');

        it('should have "timestamp" on first line', () => {
            const timestamp = lines[0];
            expect(timestamp).toMatch(/timestamp: .*/);
        });

        it('should have "principal" on second line', () => {
            expect(lines[1]).toMatch(/principal: .*/);
        });

        it('should have "type" on third line', () => {
            expect(lines[2]).toMatch(/type: .*/);
        });

        it('should have an "empty line" on fourth', () => {
            expect(lines[3]).toBe('');
        });

        it('should have "data" on fifth line', () => {
            expect(lines[4]).toMatch(/data: .*/);
        });

    });
});
