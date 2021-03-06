package Issue.Tracking.Tool.LoginSessionPoint.Api;

import Issue.Tracking.Tool.LoginSessionPoint.Domain.APIUser;
import Issue.Tracking.Tool.LoginSessionPoint.Domain.Role;
import Issue.Tracking.Tool.LoginSessionPoint.Service.SecurityService;
import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.ResponseEntity.created;

@Controller
//@RequestMapping(path = "/LoginSessionPoint")
@RequiredArgsConstructor
public class UserServiceClass {
    private final  Issue.Tracking.Tool.LoginSessionPoint.Service.UserService userService;
    private final SecurityService securityService;
    @ResponseBody
    @GetMapping("/user")
    public ResponseEntity<List<APIUser>>getUsers(){

        return ResponseEntity.ok().body(userService.getUsers());

    }


    //testing only
    @ResponseBody
    @GetMapping("role")
    public ResponseEntity<List<Role>>getALLRoles(){

        return ResponseEntity.ok().body(userService.getALLRoles());

    }

    @GetMapping("/LoginSession")

    public String LoginSession() {
        if (securityService.isAuthenticated()) {
            return "redirect:/user";
        }
        else{

            return "LoginSession";
        }

    }




    //testing only

   // @GetMapping


    //testing only
    @ResponseBody
    @PostMapping("user/save")
    public ResponseEntity<APIUser>saveUser(@RequestBody APIUser user) {
        URI uri = URI.create(ServletUriComponentsBuilder.fromCurrentContextPath().path("/LoginSessionPoint/user/save").toUriString());
        return created(uri).body(userService.saveUser(user));
    }
    @ResponseBody
    @PostMapping("role/save")
    public ResponseEntity<Role>saveRole(@RequestBody Role role) {
        URI uri = URI.create(ServletUriComponentsBuilder.fromCurrentContextPath().path("/LoginSessionPoint/role/save").toUriString());
        return created(uri).body(userService.saveRole(role));
    }
    @ResponseBody
    @PostMapping("role/addtouser")
    public ResponseEntity<?>addRoleToUser(@RequestBody RoleToUserForm form) {
         userService.addRoleToUser(form.getUsername(), form.getRoleName());
        return ResponseEntity.ok().build();
    }
    @ResponseBody
    @GetMapping("token/refresh")
    public void refreshToken(HttpServletRequest request, HttpServletResponse response) throws IOException {

        String authorizationHeader = request.getHeader(AUTHORIZATION); //

        if(authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            try {
                String refresh_token = authorizationHeader.substring("Bearer ".length()); // signature
                Algorithm algorithm = Algorithm.HMAC256("secret".getBytes()); // encryption algo

                JWTVerifier verifier = JWT.require(algorithm).build();  // Verifier object
                DecodedJWT decodedJWT = verifier.verify(refresh_token);  // Verify token
                String username = decodedJWT.getSubject();     // decode username

                APIUser user = userService.getUser(username);   // create APIUser object

                String access_token = JWT.create()
                        .withSubject(user.getUsername())
                        .withExpiresAt(new Date(System.currentTimeMillis() + 10 * 60 * 1000))  // Token Expiress at 10 mins
                        .withIssuer(request.getRequestURL().toString())
                        .withClaim("Role", user.getRoles().stream().map(Role::getName).collect(Collectors.toList()))
                        .sign(algorithm);
                Map<String, String> tokens = new HashMap<>();

                tokens.put("access_token", access_token);
                tokens.put("refresh_token", refresh_token);

                response.setContentType(MediaType.APPLICATION_JSON_VALUE);

                new ObjectMapper().writeValue(response.getOutputStream(), tokens);
            }catch (Exception exception) {

                response.setHeader("error", exception.getMessage());
                response.setStatus(FORBIDDEN.value());

                //response.sendError(FORBIDDEN.value());
                Map<String, String> error = new HashMap<>();
                error.put("error_message", exception.getMessage());

                response.setContentType(MimeTypeUtils.APPLICATION_JSON_VALUE);
                new ObjectMapper().writeValue(response.getOutputStream(), error);
            }
        } else {
            throw new RuntimeException("Refresh token is missing");
        }
    }
}

@Data
class RoleToUserForm {
    private String username;
    private String roleName;
}



