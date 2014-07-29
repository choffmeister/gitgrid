angular.module("app").filter("skipRight", () ->
  (arr, n) -> arr.slice(0, arr.length - n)
)
