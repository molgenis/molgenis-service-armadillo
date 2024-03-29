export class ApiError extends Error {
  cause: number;
  constructor(message: string, cause: number) {
    super(message);
    this.cause = cause;
    this.name = "ApiError";
  }
}
