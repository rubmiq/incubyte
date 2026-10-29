export interface ApiFieldError {
  readonly field: string;
  readonly message: string;
}

export interface ApiError {
  readonly code: string;
  readonly message: string;
  readonly fieldErrors: readonly ApiFieldError[];
}
