angular.module("app").controller("showProjectTreeController", ["$scope", "$routeParams", "restService", ($scope, $routeParams, restService) ->
  $scope.ownerName = $routeParams.ownerName
  $scope.projectName = $routeParams.projectName
  $scope.ref = $routeParams.ref
  $scope.path = $routeParams.path or ""
  $scope.pathSegments = $scope.path.split("/")

  $scope.combinePaths = (paths...) ->
    combineTwoPaths = (path1, path2) ->
      p1 = (path1 + "/" + path2).split("/")
      p2 = []
      for i in [0...p1.length]
        t = p1.shift()
        if t == ".."
          if p2.length > 0 then p2.pop()
        else if t != "." and t != ""
          p2.push(t)
      p2.join("/")
    _.reduce(paths, combineTwoPaths)

  $scope.pathSegments2 = _.chain([1..$scope.pathSegments.length])
    .map((i) -> _.take($scope.pathSegments, i))
    .filter((x) -> x.length != 1 or x[0] != "")
    .map((x) ->
      name: _.last(x)
      path: $scope.combinePaths(x.join("/"))
    )
    .value()

  restService.retrieveGitTree($scope.ownerName, $scope.projectName, $scope.ref, $scope.path)
    .success((tree) -> $scope.tree = tree)
])
