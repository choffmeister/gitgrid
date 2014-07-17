angular.module("app").controller("registerController", ["$scope", "$location", "$http", ($scope, $location, $http) ->
  $scope.userName = ""
  $scope.password = ""
  $scope.message = null

  $scope.register = () ->
    userName = $scope.userName
    password = $scope.password
    $scope.password = ""

    $http.post("/api/auth/register", { userName: userName, password: password })
      .success((res) -> $location.path("/login"))
      .error((err) ->
        $scope.message =
          type: "error"
          text: "An unknown error occured"
      )
])
