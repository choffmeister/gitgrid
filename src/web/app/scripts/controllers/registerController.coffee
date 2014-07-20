angular.module("app").controller("registerController", ["$scope", "$location", "restService", ($scope, $location, restService) ->
  $scope.userName = ""
  $scope.password = ""
  $scope.message = null

  $scope.register = () ->
    userName = $scope.userName
    password = $scope.password
    $scope.password = ""

    restService.register(userName, password)
      .success((res) -> $location.path("/login"))
      .error((err) ->
        $scope.message =
          type: "error"
          text: "An unknown error occured"
      )
])
