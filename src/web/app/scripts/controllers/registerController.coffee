angular.module("app").controller("registerController", ["$scope", "$location", "restService", ($scope, $location, restService) ->
  $scope.userName = ""
  $scope.email = ""
  $scope.password = ""
  $scope.message = null

  $scope.register = () ->
    restService.register($scope.userName, $scope.email, $scope.password)
      .success((res) ->
        $scope.password = ""
        $location.path("/login")
      )
      .error((err) ->
        $scope.password = ""
        $scope.message =
          type: "error"
          text: "An unknown error occured"
      )
])
