/**
 * Created with IntelliJ IDEA.
 * User: faissalboutaounte
 * Date: 2014-03-15
 * Time: 17:34
 * To change this template use File | Settings | File Templates.
 */
var jmaghreb = angular.module('jmaghreb', ['ngAnimate']);
jmaghreb.controller('LoginCtrl', function ($scope,$http) {
    $scope.doLogin = function(){
        $http.post("/login/"+$scope.login._id+"/"+$scope.login.password).success(function(){
            window.location.href="/cfp"
        })
    }
})

jmaghreb.controller('RegistrationCtrl', function ($scope,$http) {
    $scope.register = {};
    $scope.doRegister = function(){
        $http.post("/register",$scope.register).error(function(error){
            alert("error="+error)
        })
    }
})

jmaghreb.controller('talksCtrl', function ($scope,$http) {
    $scope.talks = {};
    $scope.selectedTalk = {};
    $http.get("/talks").success(function(data){
        $scope.talks = data[0];
    })
    $scope.edit = function(talk){
        $scope.selectedTalk = {}
        $scope.selectedTalk = talk;
        $scope.form = true;
        $scope.edition= true;
    }
    $scope.add =function(){
        $scope.selectedTalk = {}
        $scope.form = true;
        $scope.edition= false;
    }
    $scope.save = function(){
        $scope.selectedTalk.loading = true;
        $http.post("/editTalk",JSON.stringify($scope.selectedTalk)).error(function(error){

            $scope.selectedTalk.error = true;
        }).success(function(data){

                $scope.selectedTalk.loading = false;
                $scope.selectedTalk.error = false;
            })
        $scope.cancel();
    }
    $scope.cancel =function(){

        $scope.form = false;
        $scope.edition= false;
    }
})
