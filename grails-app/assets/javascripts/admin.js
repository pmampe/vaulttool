var adminModule = (function ($) {
    "use strict";

    var initModule, $body;


    //TODO: All the code needs some serious refactoring and "DRYING"
    function initVariables($container){
        $body = $('body');

        $body.on('click', '#addPolicyLink', function(){
            var $policiesContainer  = $("#policiesContainer");
            var $policyLinkLabel    = $("#policyLinkLabel");
            var $addPolicyLink      = $("#addPolicyLink");

            if($policiesContainer.hasClass('d-none')){
                utilityModule.hideMessage();

                $policiesContainer.removeClass('d-none').addClass('d-block');
                $policyLinkLabel.html("Hide policies");
                $addPolicyLink.find("span").removeClass('fa-plus').addClass('fa-times');
            } else {
                $policiesContainer.removeClass('d-block').addClass('d-none');
                $policyLinkLabel.html("Add policies");
                $addPolicyLink.find("span").removeClass('fa-times').addClass('fa-plus');
            }
            
        });
    }

    function addPolicyToSelectedPolicies(){
        $body.on('click', '[id^="policy_"]', function(){
            
            var policyName      = $(this).data('policy');
            var $policies       = $("[name='policies']");
            var currentPolicies = $policies.val();
            var policyToRemove  = "#policy_"+policyName;
            var policyId        =  'selectedPolicy_' + policyName;

            currentPolicies = addToCurrentPolicies(currentPolicies, policyName);
            $policies.val(currentPolicies);

            var $policy = createSelectedPolicy(policyId, policyName);
            $policy.data('selectedpolicy', policyName);
            $("#selectedPolicies").append($policy).append(" ");

            if($("#selectedPolicies").children('span').length === 1) {
                $("#selectedPolicies").addClass('bottom-margin-medium');
            }
            $(policyToRemove).remove();
        });

        
        $body.on('click', '[id^="editableApproleSelectablePolicy_"]', function(){
            var policyName      = $(this).data('policy');
            var appRole         = $(this).data('approle');
            var $policies       = $("[name='editableApprolePolicies_" + appRole + "']" );
            var currentPolicies = $policies.val();
            var policyToRemove  = $("#policiesContainer_" + appRole).find("#editableApproleSelectablePolicy_" + policyName + "_" + appRole);
            var policyId        = 'editableApproleSelectedPolicy_' + policyName + "_" + appRole;

            currentPolicies = addToCurrentPolicies(currentPolicies, policyName);
            $policies.val(currentPolicies);
            
            var $policy = createSelectedPolicy(policyId, policyName);
            $policy.attr('data-status', 'unsaved');
            $policy.attr('data-edappselpolicy', policyName);
            $policy.attr('data-edappselapprole', appRole);
            $("#selectedPolicies_" + appRole).append($policy).append(" ");
            $(policyToRemove).remove();

        });

    }

    function removePoliciesFromSelectedPolicies(){
        $body.on('click', '[id^="selectedPolicy_"]', function(){

            var selectedPolicy = $(this).data('selectedpolicy');
            var policyToRemove = "#selectedPolicy_" + selectedPolicy;
            var $policies      = $("[name='policies']");
            var policiesValues = $policies.val();
            var policyId       = 'policy_'+ selectedPolicy;
            var $policy        = null;

            policiesValues = removeFromCurrentPolicies(policiesValues, selectedPolicy);
            $policies.val(policiesValues);
            $(policyToRemove).remove();

            $policy = createSelectablePolicy(policyId, selectedPolicy);
            $policy.data('policy', selectedPolicy);
            $("#selectablePolicies").append($policy);

            if($("#selectedPolicies").children('span').length === 0) {
                $("#selectedPolicies").removeClass('bottom-margin-medium');
            }
        });

        $body.on('click','[id^="editableApproleSelectedPolicy_"]', function(){
            var selectedPolicy  = $(this).data('edappselpolicy');
            var appRole         = $(this).data('edappselapprole');
            var policyToRemove  = "#editableApproleSelectedPolicy_" + selectedPolicy + "_" + appRole;
            var approleSel      = "editableApprolePolicies_" + appRole;
            var $policies       = $('[name="' + approleSel +'"]');
            var policiesValues  = $policies.val();
            var policyId        = 'editableApproleSelectablePolicy_'+ selectedPolicy + "_" + appRole;
            var $policy         = null;

            policiesValues = removeFromCurrentPolicies(policiesValues, selectedPolicy);
            $policies.val(policiesValues);
            $(policyToRemove).remove();

            $policy = createSelectablePolicy(policyId, selectedPolicy);
            $policy.data('policy', selectedPolicy);
            $policy.data('approle', appRole);
            $("#selectablePolicies_" + appRole).append($policy);
        })
    }



    function createPolicy() {
        $body.on('click', '#createUpdateApproleButton', function(event){
            event.preventDefault();
            utilityModule.hideMessage();

            var appRoleName = $('#name').val();
            var form        = $("#createApproleForm").serialize();
            var $policies   = $("[name='policies']");

            callServer(form, 'createApprole')
                    .done(function(data){
                        $('#dashboard').html(data);
                        utilityModule.showMessage('info','Successfully created approle ' + appRoleName);
                        $policies.val("");
                        
                    })
                    .fail(function(data){
                        utilityModule.showMessage('error', data.responseText);
                        console.log(data.responseText);
                        $policies.val("");
                    })

        });
    }

    function updateApprole(){
        $body.on('click', '#updateApproleButton', function(event){
            event.preventDefault();
            utilityModule.hideMessage();

            var appRoleName     = $(this).data('approle');
            var $policies       =  $("#editableApprolePolicies_" + appRoleName);
            var policiesValues  = $policies.val();

            callServer({name: appRoleName, policies: policiesValues }, 'createApprole')
                    .done(function(data){
                        $('#dashboard').html(data);
                        utilityModule.showMessage('info','Successfully created approle ' + appRoleName);
                        $policies.val("");

                    })
                    .fail(function(data){
                        utilityModule.showMessage('error', data.responseText);
                        console.log(data.responseText);
                        $policies.val("");
                    })

        })
    }

    function deleteApprole(){
        $(document).off('click', '.deleteApproleLink');
        $(document).on('click', '.deleteApproleLink', function(event){
            event.preventDefault();
            utilityModule.hideMessage();

            var approle = $(this).data('approle');
            $.ajax({
                type: "POST",
                url: "/admin/deleteApprole",
                data : {approle:approle},
                success: function (data) {
                    $('#dashboard').html(data);
                    utilityModule.showMessage('info','Successfully deleted approle ' + approle);
                },
                error: function(data) {
                    utilityModule.showMessage('error', data.responseText);
                    console.log(data.responseText);
                }
            });

        });
    }

    function editApprole(){
        $body.on('click', '.editApproleLink', function(event){
            event.preventDefault();
            utilityModule.hideMessage();
            var approle = $(this).data('approle');
            var $approleView = $("#editablePolicyListItemView_" + approle);
            var $approleEditLink = $("#editableApproleListItemLink_" + approle);
            var $approlePolicyContainer = $("#approlePolicyContainer_" + approle);

            if($approlePolicyContainer.hasClass('d-none')){
                $approlePolicyContainer.removeClass('d-none').addClass('d-block');
                $approleEditLink.removeClass('d-block').addClass('d-none');
                $approleView.removeClass('d-block').addClass('d-none');
            } 
        });
    }

    function editApproleItemPolicies(){
        $body.on('click', '.editableApprolePolicyLink', function(){
            var approle = $(this).data('approle');
            var $policiesContainer = $("#policiesContainer_" + approle);
            var $approlePoliciesLinkLabel = $("#approlePoliciesLinkLabel_" + approle);

            if($policiesContainer.hasClass('d-none')){
                $policiesContainer.removeClass('d-none').addClass('d-block');
                $approlePoliciesLinkLabel.html("Hide policies");
                $(this).find("span").removeClass('fa-plus').addClass('fa-times');
            } else {
                $policiesContainer.removeClass('d-block').addClass('d-none');
                $approlePoliciesLinkLabel.html("Add policies");
                $(this).find("span").removeClass('fa-times').addClass('fa-plus');
            }
        });
    }

    function cancelEditApprole(){
        $body.on('click', '.cancelEditApproleLink', function(event){
            event.preventDefault();
            utilityModule.hideMessage();
            var approle                     = $(this).data('approle');
            var $approleView                = $("#editablePolicyListItemView_" + approle);
            var $approleEditLink            = $("#editableApproleListItemLink_" + approle);
            var $approlePolicyContainer     = $("#approlePolicyContainer_" + approle);
            var $policiesContainer          = $("#policiesContainer_" + approle);
            var $approlePoliciesLinkLabel   = $("#approlePoliciesLinkLabel_" + approle);
            var savedApprolePolicies        = $("[name='savedApprolePolicies_" + approle + "']").val();

            var $selectedPoliciesContainer = $("#selectedPolicies_" + approle);
            var policiesToRemove =  $selectedPoliciesContainer.find("span[data-status='unsaved']");  
            var $selectedPolicies = $("[name='editableApprolePolicies_" + approle +"']");
            var selectedPoliciesValues = $selectedPolicies.val();

            $selectedPoliciesContainer.html("");

            $.each(savedApprolePolicies.split(','), function(i, val){
                var policyName      = val.trim();
                var policyId        = "editableApproleSelectedPolicy_" + policyName + "_" + approle;
                var $policy         = createSelectedPolicy(policyId, policyName);
                var policyToRemove  = "#editableApproleSelectablePolicy_" + policyName + "_" + approle;

                $policy.attr('data-edappselpolicy', policyName);
                $policy.attr('data-edappselapprole', approle);
                $selectedPoliciesContainer.append($policy).append(" ");
                $(policyToRemove).remove();

            });


            $.each(policiesToRemove, function(i, val){
                var policy      = $(val).data('edappselpolicy');
                var policyId    = 'editableApproleSelectablePolicy_'+ policy + "_" + approle;

                selectedPoliciesValues = removeFromCurrentPolicies(selectedPoliciesValues, policy);
                $selectedPolicies.val(selectedPoliciesValues);
                $(val).remove();

                var $policy = createSelectablePolicy(policyId, policy);
                $policy.data('policy', policy);
                $policy.data('approle', approle);
                $("#selectablePolicies_" + approle).append($policy);

            });

            if($approlePolicyContainer.hasClass('d-block')){
                $approlePolicyContainer.removeClass('d-block').addClass('d-none');
                $approleEditLink.removeClass('d-none').addClass('d-block');
                $approleView.removeClass('d-none').addClass('d-block');
                $policiesContainer.removeClass('d-block').addClass('d-none');
                $approlePoliciesLinkLabel.html("Add policies");
                $approlePoliciesLinkLabel.closest("span").find('.fa-times').removeClass('fa-times').addClass('fa-plus');

            }
        });
    }

    function addToCurrentPolicies(policies, policyName){
        var currentPolicies = policies;
        if(currentPolicies){
            currentPolicies += "," + policyName;
        } else {
            currentPolicies += policyName
        }
        return currentPolicies;
    }

    function removeFromCurrentPolicies(policies, policyName){
        var currentPolicies = policies;
        if(/,/g.test(currentPolicies)){
            if(currentPolicies.substr(-(policyName.length)) === policyName){
                currentPolicies = currentPolicies.replace(',' + policyName, "");
            } else {
                currentPolicies = currentPolicies.replace(policyName + ',', "");
            }
        } else {
            currentPolicies = currentPolicies.replace(policyName, "");
        }
        return currentPolicies;
    }

    function createSelectedPolicy(id, policy){
        var $policy = $("<span></span>");
        var $icon   = $("<span></span>");

        $icon.addClass('fa fa-times pointer');
        $policy.append(policy);
        $policy.append(" ").append($icon);
        $policy.attr('id', id);
        $policy.data('selectedpolicy', policy);
        $policy.addClass('selectedAppRole');
        $policy.css('bottom-margin-xsmall');

        return $policy;
    }

    function createSelectablePolicy(id, policy) {
        var $policy         = $("<div></div>");
        var $policySpan     = $("<span></span>");
        var $policyStrong   = $("<strong></strong>");

        $policy.attr('id', id);
        $policy.addClass('col-3 pointer');
        $policySpan.addClass('selectableAppRole');
        $policyStrong.append(policy);
        $policySpan.append($policyStrong);
        $policy.append($policySpan);

        return $policy;
    }

    function callServer(data, url){
         return $.ajax({
             type: 'POST',
             url: '/admin/' + url,
             data: data
         });
    }

    initModule = function($container){
        initVariables($container);
        addPolicyToSelectedPolicies();
        removePoliciesFromSelectedPolicies();
        createPolicy();
        deleteApprole();
        editApprole();
        cancelEditApprole();
        editApproleItemPolicies();
        updateApprole();
    };

    return {
        initModule: initModule
    }
     
}(jQuery));

jQuery(document).ready(
        function () {
            adminModule.initModule(jQuery("body"));
        }
);

