package se.su.it.vaulttool

import grails.converters.JSON
import org.springframework.web.multipart.MultipartFile

class DashboardController {
    def vaultRestService
    def vaultService
    def utilityService

    /*def test(){
        def nisse = vaultService.deletePath(session.token, "testgroup/scrum3/")
        def byteArray = vaultService.copyPath(session.token, "systemutveckling/scrum3/")
        def result = vaultService.pastePath(session.token, "testgroup", byteArray)
        redirect(action: "index")
    }*/

    def index() {
        String selectedPath = params?.selectedPath?:""
        session.selectedPath = selectedPath
        def paths = vaultRestService.getPaths(session.token)
        List<Map<String, MetaData>> secretMetaData = []
        def secrets = vaultRestService.listSecrets(session.token, selectedPath)

        secrets.removeAll {it.endsWith("/")}
        if(secrets) {
            secrets.each {String secret ->
                secretMetaData << [secret: secret, metadata: MetaData.findBySecretKey(selectedPath+secret)]
            }
        }
        def capabilities = vaultRestService.getCapabilities(session.token, selectedPath)
        [selectedPath: selectedPath, capabilities: capabilities, paths: paths, secrets: secretMetaData]
    }

    def loadRootPaths(){
        def paths =  vaultRestService.getPaths(session.token)

        def secrets = vaultRestService.listSecrets(session.token, "")

        def rootNodes = []

        def leafs = []
        def nodes = []
        def node = null

        def isAdmin = session.group == 'sysadmin' || session.group == grailsApplication.config.vault.sysadmdevgroup
        
        secrets.each {secret ->

            if(secret.endsWith("/")){
                node = ['id': secret.replace("/",""), 'text': secret.replace("/",""), admin: isAdmin, type: 'pathNode', 'children': true, 'icon': 'fa fa-folder']
                nodes.add(node)
            }  else {
                node =  ['id': 'leaf_' + secret, 'text': secret, admin: isAdmin, type: 'leafNode', 'children': false, 'icon':'fa fa-key', 'a_attr':['data-secretkey': secret]]
                leafs.add(node)
            }
            rootNodes = nodes + leafs
         }

        def rootNode = [['id': 'root', 'text': 'Root', admin: isAdmin, type: 'rootNode','children': rootNodes, 'icon':'fa fa-home fa-lg','state':['opened': true]]]

        return render(rootNode as JSON)
    }


    def loadChildren(){
        def secrets = vaultRestService.listSecrets(session.token, params['id'].toString().replaceAll("_","/"))
        def childNodes = []

        def isAdmin = session.group == 'sysadmin' || session.group == grailsApplication.config.vault.sysadmdevgroup

        secrets.each {secret ->
            def node = null
            if(secret.endsWith("/")){
                node = ['id':params['id'] + '_' + secret.replace("/",""), parent:params['id'], 'text': secret.replace("/",""), admin: isAdmin, 'type':'pathNode', 'children': true, 'icon' : 'fa fa-folder']
             }  else {
                node =  ['id':'leaf_' + params['id'] + '_' + secret.replace("/",""), parent:params['id'], 'text': secret.replace("/",""), admin: isAdmin, type: 'leafNode', 'children': false, 'icon':'fa fa-key', 'a_attr':['data-secretkey': params['id'].toString().replaceAll("_","/") +'/' + secret ]]
            }

            childNodes.add(node)
        }
        return render (childNodes as JSON)
    }

    def deletePath(String path){
        String pathToDelete = params['path'] as String
        
        if(pathToDelete.empty){
            redirect(actionName: "index")
            return
        }

        Map<String, String> result = vaultService.deletePath(session.token, pathToDelete)
        return render (result as JSON)
    }

    def copyPastePath() {
        String path = params['path'] as String
        String destination = params['destination'] as String

        if(path.empty){
            redirect(actionName: "index")
            return
        }

        Byte[] zipByteArray = vaultService.copyPath(session.token, path)
        Map<String, String> result = vaultService.pastePath(session.token, destination, zipByteArray)

        return render (result as JSON)

    }

    def handlePaths(){
        String fromPath = params.fromPath ? params.fromPath as String : ''
        String toPath = params.toPath ? params.toPath as String : ''
        Boolean deletePath = params.deletePath ? true : false

        Byte[] zipByteArray = []
        Map<String, String> result = [:]
        
        if(fromPath && toPath){
            zipByteArray = vaultService.copyPath(session.token, fromPath)
            result = vaultService.pastePath(session.token, toPath, zipByteArray)

            if(deletePath){
                result << vaultService.deletePath(session.token, fromPath)
            }
        }

        if(fromPath && !toPath){
            result << vaultService.deletePath(session.token, fromPath)
        }

        return render (result as JSON)
    }

    def search() {
        String secret = params?.secret?:""
        if(secret.empty) {
            redirect(actionName: "index")
            return
        }
        def keyTree = vaultRestService.getSecretTree(session.token)

        List<MetaData> metaDatas = MetaData.findAllBySecretKeyInList(keyTree).findAll{it.secretKey.contains(secret) || (it.title?it.title.contains(secret):false) || (it.description?it.description.contains(secret):false)}
        if(!metaDatas){
            flash.warning ='No secrets found'
            redirect(action: "index")
            return
        }
        [metadatas: metaDatas]

    }

    def secret() {
        String key = params.key?:null
        if(!key) {
            String errorMsg = "Failed when trying to read secret. Error was: No key supplied."
            log.error(errorMsg)
            flash.error = errorMsg
            redirect(action: "index")
            return
        }

        Expando response = vaultRestService.getSecret(session.token, key)
        if(response.status) {
            String errorMsg = "Failed when trying to read secret ${key}. Error was: Permission denied."
            log.error(errorMsg)
            flash.error = errorMsg
            redirect(action: "index")
            return
        } else if(!response.entry) {
            String errorMsg = "Failed when trying to read secret ${key}. Error was: secret not found."
            log.error(errorMsg)
            flash.error = errorMsg
            redirect(action: "index")
            return
        }

        MetaData metaData = MetaData.findBySecretKey(key)
        [secret: response.entry, metadata: metaData]
    }

    def updateSecret() {
        String key = params?.key?:null
        if(!key) {
            String errorMsg = "Failed when trying to update secret. Error was: No key supplied."
            log.error(errorMsg)
            flash.error = errorMsg
            redirect(actionName: "index")
            return
        }
        Expando response = vaultRestService.getSecret(session.token, key)
        if(response.status) {
            String errorMsg = "Failed when trying to read secret ${key}. Error was: ${response.status}"
            log.error(errorMsg)
            flash.error = errorMsg
            redirect(action: "index")
            return
        } else if(!response.entry) {
            String errorMsg = "Failed when trying to read secret ${key}. Error was: secret not found."
            log.error(errorMsg)
            flash.error = errorMsg
            redirect(action: "index")
            return
        }
        MetaData metaData = MetaData.findOrCreateBySecretKey(key)
        metaData.secretKey      = key
        metaData.title          = params?.title?:""
        metaData.description    = params?.description?:""

        Entry entry = response.entry
        entry.key           = key
        entry.userName      = params?.userName?:""
        entry.pwd           = params?.password?:""

        Map response2 = vaultRestService.putSecret(session.token, key, entry)
        if(response2) {
            String errorMsg = "Failed when trying to update secret ${key}. Error was: ${response2.status?:'Unknown Error'}"
            log.error(errorMsg)
            flash.error = errorMsg
        } else {
            flash.message = "Successfully updated secret ${key}"
            metaData.save(flush: true)
        }
        return redirect(action: "secret", params: [key: key])
    }

    def createSecret() {
        String key = params?.selectedPath?:""
        String path = params?.path?:""
        String secret = params?.secret?:""
        if(secret.empty) {
            String errorMsg = "Failed when trying to create secret. Error was: No key supplied."
            log.error(errorMsg)
            flash.error = errorMsg
            redirect(actionName: "index")
            return
        }
        if(path.length() > 0) {
            key += path  + "/"  + secret
        } else {
            key += secret
        }
        def secretTree = vaultRestService.getSecretTree(session.token)
        if(secretTree.contains(key)) {
            String errorMsg = "Failed when trying to create secret ${key}. Error was: Secret already exist"
            log.error(errorMsg)
            flash.error = errorMsg
            redirect(actionName: "index")
            return
        }
        Entry entry = new Entry()
        entry.key = key
        Map response = vaultRestService.putSecret(session.token, key, entry)
        if(response) {
            String errorMsg = "Failed when trying to create secret ${key}. Error was: ${response.status?:'Unknown Error'}"
            log.error(errorMsg)
            flash.error = errorMsg
            return redirect(action: "index")
        }
        MetaData metaData = new MetaData()
        metaData.secretKey = key
        metaData.title = ""
        metaData.description = ""
        metaData.fileName = ""
        metaData.save(flush: true)
        flash.message = "Successfully created secret ${key}"
        return redirect(action: "secret", params: [key: key])
    }

    def delete() {
        String key = params?.key?:null
        if(!key) {
            String errorMsg = "Failed when trying to delete secret. Error was: No secret supplied.}"
            log.error(errorMsg)
            flash.error = errorMsg
            redirect(actionName: "index")
            return
        }

        Map response = vaultRestService.deleteSecret(session.token, key)
        if(response) {
            String errorMsg = "Failed when trying to delete secret ${key}. Error was: ${response.status?:'Unknown Error'}"
            log.error(errorMsg)
            flash.error = errorMsg
            return redirect(action: "secret", params: [key: key])
        }
        MetaData metaData = MetaData.findBySecretKey(key)
        if(metaData) {
            metaData.delete(flush: true)
        }
        flash.message = "Successfully deleted secret ${key}"
        redirect(actionName: "index")
    }

    def upload() {
        String key = params?.key?:null
        if(!key) {
            String errorMsg = "Failed when trying to upload file. Error was: No secret supplied"
            log.error(errorMsg)
            flash.error = errorMsg
            redirect(actionName: "index")
            return
        }

        Expando response = vaultRestService.getSecret(session.token, key)
        if(response.status) {
            String errorMsg = "Failed when trying to read secret ${key}. Error was: ${response.status}"
            log.error(errorMsg)
            flash.error = errorMsg
            redirect(action: "index")
            return
        } else if(!response.entry) {
            String errorMsg = "Failed when trying to read secret ${key}. Error was: secret not found."
            log.error(errorMsg)
            flash.error = errorMsg
            redirect(action: "index")
            return
        }

        MultipartFile f = request.getFile('attachment')
        if (f.empty) {
            String errorMsg = "Failed when trying to upload file for secret ${key}. Error was: File not found in request."
            log.error(errorMsg)
            flash.error = errorMsg
            redirect(actionName: "index")
            return
        }

        Entry entry = response.entry
        entry.binaryData = f.bytes
        Map response2 = vaultRestService.putSecret(session.token, key, entry)
        if(response2) {
            String errorMsg = "Failed when trying to upload file for secret ${key}. Error was: ${response2.status?:'Unknown Error'}"
            log.error(errorMsg)
            flash.error = errorMsg
        } else {
            MetaData metaData = MetaData.findBySecretKey(key)
            metaData.fileName = f.originalFilename
            metaData.save(flush: true)
            flash.message = "Successfully uploaded file to secret ${key}"
        }
        return redirect(action: "secret", params: [key: key])
    }

    def download() {
        String key = params?.key?:null
        if(!key) {
            String errorMsg = "Failed when trying to download file. Error was: No secret supplied"
            log.error(errorMsg)
            flash.error = errorMsg
            redirect(actionName: "index")
            return
        }

        Expando response2 = vaultRestService.getSecret(session.token, key)
        if(response2.status) {
            String errorMsg = "Failed when trying to read secret ${key}. Error was: ${response2.status}"
            log.error(errorMsg)
            flash.error = errorMsg
            redirect(action: "index")
            return
        } else if(!response2.entry) {
            String errorMsg = "Failed when trying to read secret ${key}. Error was: secret not found."
            log.error(errorMsg)
            flash.error = errorMsg
            redirect(action: "index")
            return
        }
        Entry entry = response2.entry
        MetaData metaData = MetaData.findBySecretKey(key)
        response.setContentType("application/octet-stream")
        response.setHeader("Content-disposition", "filename=\"${metaData.fileName}\"")
        response.setContentLength(entry.binaryData.size())
        response.outputStream << entry.binaryData
    }

    def deleteFile() {
        String key = params?.key?:null
        if(!key) {
            String errorMsg = "Failed when trying to delete file. Error was: No secret supplied"
            log.error(errorMsg)
            flash.error = errorMsg
            redirect(actionName: "index")
            return
        }

        Expando response = vaultRestService.getSecret(session.token, key)
        if(response.status) {
            String errorMsg = "Failed when trying to read secret ${key}. Error was: ${response.status}"
            log.error(errorMsg)
            flash.error = errorMsg
            redirect(action: "index")
            return
        } else if(!response.entry) {
            String errorMsg = "Failed when trying to read secret ${key}. Error was: secret not found."
            log.error(errorMsg)
            flash.error = errorMsg
            redirect(action: "index")
            return
        }
        MetaData metaData = MetaData.findBySecretKey(key)
        if(!metaData) {
            String errorMsg = "Failed when trying to read metadata for secret ${key}. Error was: metadata not found."
            log.error(errorMsg)
            flash.error = errorMsg
            redirect(action: "index")
            return
        }
        metaData.secretKey      = key
        metaData.fileName       = ""

        Entry entry = response.entry
        entry.key           = key
        entry.binaryData    = "".getBytes()

        Map response2 = vaultRestService.putSecret(session.token, key, entry)
        if(response2) {
            String errorMsg = "Failed when trying to update secret ${key}. Error was: ${response2.status?:'Unknown Error'}"
            log.error(errorMsg)
            flash.error = errorMsg
        } else {
            flash.message = "Successfully updated secret ${key}"
            metaData.save(flush: true)
        }
        return redirect(action: "secret", params: [key: key])
    }
}
