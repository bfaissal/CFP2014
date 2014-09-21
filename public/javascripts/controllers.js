/**
 * Created with IntelliJ IDEA.
 * User: faissalboutaounte
 * Date: 2014-03-15
 * Time: 17:34
 * To change this template use File | Settings | File Templates.
 */
var jmaghreb = angular.module('jmaghreb', ['ngAnimate', 'ngRoute']);

jmaghreb.controller('MainController', function ($scope, $route, $routeParams, $location) {
    $scope.$route = $route;
    $scope.$location = $location;
    $scope.$routeParams = $routeParams;
})
jmaghreb.controller('LoginCtrl', function ($scope, $http) {
    $scope.showMessages = "hidden";
    $scope.errorClass = function(field){
        if(!registrationForm[field].$invalid){
            return "error";
        }
        else{
            return "error";
        }
    }
    $scope.doLogin = function () {
        $scope.showMessages = "hidden";
        try{
        $http.post("/login",$scope.login).success(function (data) {
            if(data.admin == true){
                window.location.href = "/admin"
            }
            else{
                window.location.href = "/proposals"
            }
        }).error(function(){
                $scope.showMessages = "shown";
            })
        }catch(e){
            $scope.showMessages = "shown";
        }
    }
})



jmaghreb.controller('fpCtrl', function ($scope, $http) {
    $scope.showMessages = "hidden";
    $scope.showSuccessMessages = "hidden";
    $scope.errorClass = function(field){
        if(!registrationForm[field].$invalid){
            return "error";
        }
        else{
            return "error";
        }
    }
    $scope.submit = function () {
        $scope.showSuccessMessages = "hidden";
        $scope.showMessages = "hidden";
        try{
            $http.post("/fp/"+$scope.login._id).success(function (data) {
                $scope.showSuccessMessages = "shown";
            }).error(function(){
                    $scope.showMessages = "shown";
                })
        }catch(e){
            $scope.showMessages = "shown";
        }
    }
})

jmaghreb.controller('MainCtrl', function ($scope,$rootScope, $http) {
    $rootScope.loginShow = true;
    $rootScope.fpShow = false;
    $scope.goFp = function () {
        $rootScope.fpShow = true;
    }
    $scope.cancelFp = function () {
        $rootScope.fpShow = false;
    }
    $scope.goRegister = function () {
        $rootScope.loginShow = false;
    }
    $scope.cancel = function () {
        $rootScope.loginShow = true;
        $rootScope.register = {};
    }
    $rootScope.disabled = function(disabled){
        if(disabled)
            return "disabled";
        else
            return "n";
    }
})


jmaghreb.controller('AdminSpeakerCtrl', function ($scope,$rootScope, $http,$timeout) {

    $rootScope.register = {};
    $scope.hisTalks = [];
    $scope.disableSave = false;
    $http.get("/config").success(function (data) {
        $scope.config = data;
    })
    $scope.addTalk = function(){
        $scope.hisTalks.push({});
    }
    $scope.disabled = function(disabled){
        if(disabled)
            return "disabled";
        else
            return "";
    }
    $scope.updateValue = function(item,type, original,field){

        function updateLabel(list){

            if(item.value == ""){
                if(original && field){
                    delete original[field]
                }
            }
            else{
                for(a in list){

                    if(list[a].value == item.value){

                        item.label = list[a].label;
                        break;
                    }
                }
            }
        }
        switch (type) {
            case 'lang':
                updateLabel($scope.config.languages)
                break;
            case 'confDays':
                updateLabel($scope.config.confDays)
                break;
            case 'room':
                updateLabel($scope.config.rooms)
                break;
            case 'type':
                updateLabel($scope.config.sessionTypes)
                break;
            case 'track':
                updateLabel($scope.config.tracks)
                break;
            case 'exp':
                updateLabel($scope.config.audienceExperiences)
                break;
        }
    }
    $scope.save = function () {
        $scope.disableSave = true;
        var speakerWithTalks = {"speaker":$scope.register,"talks":$scope.hisTalks}
        $http.post("/adminCreateSpeaker", speakerWithTalks).error(function (error) {
            alert(error)
        }).success(function (data) {
                $scope.disableSave = false;
                $rootScope.saveSuccess = true;
                $rootScope.loginShow = true;
                $rootScope.register = {};
                $scope.hisTalks = [];
                $scope.registrationForm.$setPristine();
                alert("Saved successfully ! ");
                $timeout(function(){$rootScope.saveSuccess = false;},6000)
            })
    }

})

jmaghreb.controller('RegistrationCtrl', function ($scope,$rootScope, $http,$timeout) {

    $rootScope.register = {};
    $scope.disableSave = false;
    $scope.save = function () {
        $scope.disableSave = true;
        $http.post("/register", $scope.register).error(function (error) {
            alert(error)
        }).success(function (data) {
                $scope.disableSave = false;
                $rootScope.saveSuccess = true;
                $rootScope.loginShow = true;
                $rootScope.register = {};
                $scope.registrationForm.$setPristine();
                $timeout(function(){$rootScope.saveSuccess = false;},6000)
            })
    }

})

jmaghreb.controller('ProfileCtrl', function ($scope, $http, $timeout) {
    $scope.saveSuccess = false;

    $scope.initProfile = function(){
        $http.get("/connectedUser").success(function (data) {
            $scope.register = data
        })
    }
    $scope.initProfile();
    $scope.editProfile = function () {
        $http.post("/saveProfile",$scope.register).success(function (data) {
            $scope.saveSuccess = true;
            $timeout(function(){$scope.saveSuccess = false;},3000)
        })
    }
})
jmaghreb.controller('RevProfileCtrl', function ($scope, $http, $timeout) {
    $scope.saveSuccess = false;

    $scope.initProfile = function(){
        $http.get("/connectedUser").success(function (data) {
            $scope.register = data
        })
    }
    $scope.initProfile();
    $scope.editProfile = function () {
        $http.post("/saveReviewer",$scope.register).success(function (data) {
            $scope.saveSuccess = true;
            $timeout(function(){$scope.saveSuccess = false;},3000)
        })
    }
})

jmaghreb.controller('talksCtrl', function ($scope, $http) {
    $scope.talks = {};
    $scope.selectedTalk = {};
    $scope.predicate = 'title'
    $scope.predicateList ='order'
    $scope.reverse = false;

    $http.get("/config").success(function (data) {
        $scope.config = data;
    })
    $http.get("/talks").success(function (data) {
        $scope.talks = data;
    })
    $scope.edit = function (talk) {
        $scope.selectedTalk = {}
        $scope.selectedTalk = talk;
        $scope.form = true;
        $scope.edition = true;
    }
    $scope.add = function () {
        $scope.selectedTalk = {}
        $scope.form = true;
        $scope.edition = false;
        $scope.selectedTalk.status = 1;
    }
    $scope.save = function () {
        $scope.selectedTalk.loading = true;
        $http.post("/editTalk", JSON.stringify($scope.selectedTalk)).error(function (error) {

            $scope.selectedTalk.error = true;
        }).success(function (data) {
                $scope.selectedTalk.loading = false;
                $scope.selectedTalk.error = false;
                if (data.data) {
                    $scope.talks.push(data.data)
                }
            })
        $scope.cancel();
    }

    $scope.changeStatus = function (talk, status) {
        var message = status == 2 ? 'Do you want to complete this talk ?' : 'Do you want to delete this talk ?'
        if (status == 1 || confirm(message)) {
            $scope.selectedTalk = talk;
            $scope.selectedTalk.status = status;
            $scope.save();
        }
    }

    $scope.cancel = function () {

        $scope.form = false;
        $scope.edition = false;
    }

    $scope.updateValue = function(item,type){
        function updateLabel(list){
            for(a in list){
                if(list[a].value == item.value){
                    item.label = list[a].label;
                    break;
                }
            }
        }
        switch (type) {
            case 'lang':
                updateLabel($scope.config.languages)
                break;
            case 'room':
                updateLabel($scope.config.rooms)
                break;
            case 'type':
                updateLabel($scope.config.sessionTypes)
                break;
            case 'track':
                updateLabel($scope.config.tracks)
                break;
            case 'exp':
                updateLabel($scope.config.audienceExperiences)
                break;
        }
    }
    $scope.deleteSpeaker = function(index){
        if(confirm("Are you sure you want to delete this speaker ?")){
            //delete $scope.selectedTalk.otherSpeakers[index]
            delete $scope.selectedTalk.otherSpeakers.splice(index,1)
        }
    }
    $scope.findSpeaker = function(speaker){

        $http.get("/speaker/"+speaker.id).success(function (data) {
            speaker.fname = data.fname
            speaker.lname = data.lname
            speaker.image = data.image
            speaker.bio = data.bio
            speaker.twitter = data.twitter
            speaker.id = data._id.$oid
        })
    }
    $scope.addSpeaker = function(){
        var speaker = {};
        speaker.lname='';
        if(!$scope.selectedTalk.otherSpeakers){
            $scope.selectedTalk.otherSpeakers = [];
        }
        $scope.selectedTalk.otherSpeakers.push(speaker)
    }
    $scope.disabled = function(disabled){
        if(disabled)
            return "disabled";
        else
            return "";
    }
})

jmaghreb.controller('AdminCtrl', function ($scope, $http,$timeout) {
    $scope.config = {};
    $scope.talks = {};
    $scope.predicate = 'order'
    saveSuccess = false;
    $scope.reverse = false;
    $scope.hours = [];

    $scope.minutes = [];
    $scope.minutes.push("00")
    for(i= 1; i <12 ; i++) {
        $scope.minutes.push(i*5)
    }
    for(i = 8; i < 20 ; i++){
        if(i < 10)
            $scope.hours.push("0"+i)
        else
            $scope.hours.push(i)

    }
    $http.get("/config").success(function (data) {
        $scope.config = data;
        $scope.initLists();
    })
    $http.get("/allTalks").success(function (data) {
        $scope.talks = data.talks;

    })
    $scope.initLists = function () {
        if (!$scope.config.languages) $scope.config.languages = []
        if (!$scope.config.confDays) $scope.config.confDays = []
        if (!$scope.config.rooms) $scope.config.rooms = []
        if (!$scope.config.sessionTypes) $scope.config.sessionTypes = []
        if (!$scope.config.tracks) $scope.config.tracks = []
        if (!$scope.config.audienceExperiences) $scope.config.audienceExperiences = []
        if (!$scope.config.reviewers) $scope.config.reviewers = []
    }
    $scope.initLists();
    $scope.addLang = function (type) {
        switch (type) {
            case 'lang':
                $scope.config.languages.push({});
                break;
            case 'confDays':
                $scope.config.confDays.push({});
                break;
            case 'room':
                $scope.config.rooms.push({});
                break;
            case 'type':
                $scope.config.sessionTypes.push({});
                break;
            case 'track':
                $scope.config.tracks.push({});
                break;
            case 'exp':
                $scope.config.audienceExperiences.push({});
                break;
            case 'reviewers':
                $scope.config.reviewers.push({});
                break;
            default:
                $scope.selectedVar = 'bbbbxbxbxb';
        }
    }

    $scope.save = function (onSuccess) {
        $http.post("/saveConfig", JSON.stringify($scope.config)).error(function (error) {
        })
            .success(function (data) {
                $scope.config = data.data;
                if (onSuccess) {
                    onSuccess()
                }
                $scope.saveSuccess = true;
                $timeout(function(){$scope.saveSuccess = false;},3000)
            })
    }
    $scope.activateRev = function (list,item, index) {
        if (confirm("Are you sure you want to send and email to this reviewer ?")) {
            for ( a in list){
                if(list[a].label == item.label){
                    index = a ;
                }
            }
            $http.post("/createRev/"+item.email).success(function(){
                item.emailSent= true;
                $scope.save()
            }).error(function(data){
                    alert(data);
                })

        }
    }
    $scope.delete = function (list,item, index) {
        if (confirm("Are you sure you want to delete this line ?")) {
            for ( a in list){
                if(list[a].label == item.label){
                    index = a ;
                }
            }
            list.splice(index, 1);
            $scope.save()
        }
    }

    // talks admin
    $scope.predicate = 'title'
    $scope.predicateList ='order'


    $scope.edit = function (talk) {
        $scope.selectedTalk = {}
        $scope.selectedTalk = talk;
        $scope.form = true;
        $scope.edition = true;
    }
    $scope.add = function () {
        $scope.selectedTalk = {}
        $scope.form = true;
        $scope.edition = false;
        $scope.selectedTalk.status = 1;
    }
    $scope.schedule = function (talk) {
        $scope.selectedTalk = talk;
        $scope.saveTalk();
    }
    $scope.saveTalk = function (sendEmail,acceptSpeaker) {
        if(!sendEmail) sendEmail = false;
        if(!acceptSpeaker) acceptSpeaker = false;
        $scope.selectedTalk.loading = true;

        $http.post("/adminEditTalk?email="+sendEmail+"&acceptSpeaker="+acceptSpeaker, JSON.stringify($scope.selectedTalk)).error(function (error) {

            $scope.selectedTalk.error = true;
        }).success(function (data) {
                $scope.selectedTalk.loading = false;
                $scope.selectedTalk.error = false;
                if (data.data) {
                    $scope.talks.push(data.data)
                }
            })
        $scope.cancel();
    }
    $scope.changeStatus = function (talk, status,isEmailSent) {
        var message = status == 2 ? 'Do you want to complete this talk ?' : 'Do you want to delete this talk ?'
        if(!isEmailSent) isEmailSent = false;
        if (status == 1 || confirm(message)) {
            $scope.selectedTalk = talk;
            $scope.selectedTalk.status = status;
            $scope.saveTalk(isEmailSent ,status == 3);
        }
    }
    $scope.cancel = function () {

        $scope.form = false;
        $scope.edition = false;
    }

    $scope.updateValue = function(item,type,original,field){

        function updateLabel(list){
            for(a in list){
                if(item.value == "" || item.value == undefined ){
                    if(original && field){
                        delete original[field]
                    }
                }
                else{
                    for(a in list){

                        if(list[a].value == item.value){

                            item.label = list[a].label;
                            break;
                        }
                    }
                }
            }
        }
        switch (type) {
            case 'lang':
                updateLabel($scope.config.languages)
                break;
            case 'confDays':
                updateLabel($scope.config.confDays)
                break;
            case 'room':
                updateLabel($scope.config.rooms)
                break;
            case 'type':
                updateLabel($scope.config.sessionTypes)
                break;
            case 'track':
                updateLabel($scope.config.tracks)
                break;
            case 'exp':
                updateLabel($scope.config.audienceExperiences)
                break;
        }
    }

})

jmaghreb.config(function ($routeProvider) {
    $routeProvider.when('p1', { templateUrl: 'partial1_.html', controller: 'talksCtrl' })
        .when('p2', { templateUrl: 'partial2_.html', controller: 'ProfileCtrl' });
})

jmaghreb.filter('notScheduled', function() {
    return function(talks,scheduled) {
        var out = [];
        if(scheduled == 1){
        for (aTalk in talks){
            try{
                if(talks[aTalk].day && talks[aTalk].day.value != "" && talks[aTalk].room && talks[aTalk].room.value != ""){
                out.push(talks[aTalk]);
            }
            }catch(e){}
        }
        }
        else{
            if(scheduled == 2){
                for (aTalk in talks){
                    try{
                        console.info(talks[aTalk].day.value+" => "+(!talks[aTalk].day || !talks[aTalk].room || talks[aTalk].day.value))
                        if(!talks[aTalk].day || !talks[aTalk].room || !talks[aTalk].room.value || talks[aTalk].room.value == ""
                            || !selectedTalk.from.h || selectedTalk.from.h==""
                            ||!selectedTalk.from.m || selectedTalk.from.m == ""
                            ||!selectedTalk.to.h || selectedTalk.to.h == ""
                            ||!selectedTalk.to.m || selectedTalk.from.m == ""){

                            out.push(talks[aTalk]);
                        }
                    }catch(e){}
                }
            }
            else{
                out = talks;
            }

        }
        return out;
    }
});