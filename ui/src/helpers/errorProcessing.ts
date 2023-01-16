export function processErrorMessages(error: string, router: { push: (arg0: string) => Promise<any>; go: (arg0: number) => void; }) {
  if (
    error === "Unauthorized" ||
    error.toString() ===
      "ConnectionError: Full authentication is required to access this resource"
  ) {
    router.push("/login").then(() => {
      router.go(0);
    });
    return error;
  } else {
    return `Could not load projects: ${error}.`;
  }
}
