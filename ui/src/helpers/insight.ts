/**
 * Get line indices of lines containing given string.
 *
 * If all lines has searchFor an empty list [] is returned.
 *
 * @param lines Array<string>
 * @param searchFor string
 */
export function matchedLineIndices(
  lines: Array<string>,
  searchFor: string
): number[] {
  let matchedLines = lines
    .map((v: string, i: number) =>
      v.toLowerCase().includes(searchFor) ? i : -1
    )
    .filter((v) => v > -1);

  return matchedLines;
}
