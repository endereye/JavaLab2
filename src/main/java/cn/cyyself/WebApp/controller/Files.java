package cn.cyyself.WebApp.controller;

import cn.cyyself.WebApp.service.FileIOService;
import cn.endereye.framework.ioc.annotations.InjectSource;
import cn.endereye.framework.ioc.annotations.InjectTarget;
import cn.endereye.framework.web.WebResponse;
import cn.endereye.framework.web.annotations.RequestController;
import cn.endereye.framework.web.annotations.RequestEndpoint;
import cn.endereye.framework.web.annotations.RequestParam;
import cn.endereye.framework.web.model.UploadFiles;
import org.apache.commons.fileupload.FileItem;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Map;

@InjectSource
@RequestController("/files/")
public class Files {
    @InjectTarget
    private FileIOService fileIO;

    @RequestEndpoint(".*")
    public WebResponse doAGet(HttpServletRequest req) throws IOException {
        String path     = URLDecoder.decode(req.getRequestURI().substring("/files".length()), "UTF-8");
        String new_path = fileIO.getPath(path);
        if (!new_path.equals(path)) return WebResponse.redirect("/files" + new_path);
        if (!path.endsWith("/")) {
            //is file
            return WebResponse.raw(fileIO.getFile(path));
        } else {
            //is directory
            Map<String, Object> jsp_params = new HashMap<>();
            jsp_params.put("path", fileIO.getPath(path));
            jsp_params.put("files", fileIO.listFile(path));
            return WebResponse.jsp(jsp_params, "/listFile.jsp");
        }
    }

    @RequestEndpoint(value = ".*", method = "POST")
    public WebResponse doAPost(@RequestParam("action") String action, @RequestParam("dir_name") String dir_name, UploadFiles files, HttpServletRequest req) throws IOException {
        String path     = URLDecoder.decode(req.getRequestURI().substring("/files".length()), "UTF-8");
        String new_path = fileIO.getPath(path);
        if (path.equals(new_path)) {
            if (action == null && files != null) {//file upload
                boolean result = true;
                for (FileItem x : files.FileList) result &= fileIO.upload(path, x);
                if (result) return WebResponse.string("{\"status\":200,\"msg\":\"Upload Success\"}");
                else return WebResponse.string("{\"status\":404,\"msg\":\"Upload Failed\"}");
            } else if ("mkdir".equals(action)) {
                if (fileIO.mkdir(path, dir_name))
                    return WebResponse.string("{\"status\":200,\"msg\":\"ok\"}");
                else return WebResponse.string("{\"status\":500,\"msg\":\"Server Error\"}");
            } else {
                return WebResponse.string("{\"status\":500,\"msg\":\"Unknow action\"}");
            }
        } else return WebResponse.string("{\"status\":403,\"msg\":\"path unavailable\"}");
    }
}