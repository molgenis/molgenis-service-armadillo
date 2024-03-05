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
  if (searchFor.length == 0) return [];

  let matchedLines = lines
    .map((v: string, i: number) =>
      v.toLowerCase().includes(searchFor) ? i : -1
    )
    .filter((v) => v > -1);
  return matchedLines;
}

export function auditJsonLinesToLines(lines: string[]): string[] {
  // From https://docs.spring.io/spring-boot/docs/current/api/org/springframework/boot/actuate/audit/AuditEvent.html
  const auditBaseFields = ["timestamp", "principal", "type"];
  const mapper = (k: string, v: string | number) => `${k}: ${v}\n`;

  return lines.map((line) => {
    let html = "";
    const record = JSON.parse(line);
    auditBaseFields.forEach((field) => {
      if (record[field]) {
        html += mapper(field, record[field]);
      }
    });

    return (
      html + "\n" + mapper("data", "\n" + JSON.stringify(record.data, null, 2))
    );
  });
}
