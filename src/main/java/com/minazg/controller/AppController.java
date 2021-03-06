package com.minazg.controller;

import java.io.*;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;

import com.minazg.exception.UserNotFoundException;
import com.minazg.model.UserRole;
import com.minazg.service.UserRoleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.context.support.MessageSourceAccessor;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.ui.ModelMap;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import org.springframework.data.domain.Pageable;

import com.minazg.model.User;
import com.minazg.service.UserService;
import org.springframework.web.multipart.MultipartFile;

@Controller
@SessionAttributes(value = {"roles","pages","page","pageSize","totalRecords"})
public class AppController {

	@Autowired
	UserRoleService userRoleService;

	@Autowired
	UserService userService;

	@Autowired
	MessageSourceAccessor messageSource;

	//@Value("#{'${upload.locations}'.split(',')}")
	//private List<String> uploadLocations;

	/**
	 * This method will provide UserProfile list to views
	 */
	@ModelAttribute("roles")
	public List<UserRole> initializeProfiles(Model model) {

		return userRoleService.findAll();
	}

	/**
	 * This method will list all existing users.
	 */
	@RequestMapping(value = { "/", "/list" }, method = RequestMethod.GET)
	public String listUsers(ModelMap model, @PageableDefault(size = 10) Pageable pageable) {

		List<User> users = userService.findAllUsers(pageable);

		int size = userService.totalRecord();
		int pages = (size/10) + (size % 10 > 0 ? 1 : 0);
		model.addAttribute("totalRecords",size);
		model.addAttribute("pages", pages);

		model.addAttribute("prevPage",pageable.getPageNumber());
		model.addAttribute("nextPage",pageable.getPageNumber() + 1);
		model.addAttribute("pageSize", pageable.getPageSize());


		model.addAttribute("users", users);
		model.addAttribute("loggedinuser", getPrincipal());

		return "user/userslist";
	}

	@GetMapping("/user-search")
	public String searchUser(@RequestParam("q") String query,
							 @PageableDefault(size = 10) Pageable pageable, Model model){

		List<User> matchedUser = userService.filterUserByCriteria(query,pageable);
		model.addAttribute("users", matchedUser);

		int size = 0;
		if(query.isEmpty()){
			size = userService.totalRecord();
		}else {
			size = matchedUser.size();
		}
		int pages = (size/10) + (size % 10 > 0 ? 1 : 0);
		model.addAttribute("totalRecords",size);
		model.addAttribute("pages", pages);

		model.addAttribute("prevPage",pageable.getPageNumber());
		model.addAttribute("nextPage",pageable.getPageNumber() + 1);
		model.addAttribute("pageSize", pageable.getPageSize());

		return "user/userslist";

	}

	/**
	 * This method will provide the medium to add a new user.
	 */
	@RequestMapping(value = { "/newuser" }, method = RequestMethod.GET)
	public String newUser(@ModelAttribute("user") User user, Model model) {
		model.addAttribute("edit", false);
		model.addAttribute("loggedinuser", getPrincipal());
		return "user/registration";
	}

	/**
	 * This method will be called on form submission, handling POST request for
	 * saving user in database. It also validates the user input
	 */
	@PostMapping(value = { "/newuser" })
	public String saveUser(@Valid User user, BindingResult result,
						   ModelMap model, HttpServletRequest request) {

		if (result.hasErrors()) {
			return "user/registration";
		}

		userService.saveUser(user);

		MultipartFile productImage = user.getUserProfPic();

		String[] uploadLocations = messageSource
				.getMessage("upload.locations").split(",");

		String[] staticImgPaths = messageSource
				.getMessage("static.img.paths").split(",");

		String imageName = user.getId() + ".png";

		String rootDirectory = request.getSession().getServletContext().getRealPath("/");

		File tempFile = null; //new File(rootDirectory+"static\\img\\"+ imageName);

		for (String staticImgPath : staticImgPaths){
			if(new File(rootDirectory + staticImgPath).exists()){
				tempFile = new File(rootDirectory + staticImgPath + imageName);
				break;
			}
		}

		File permanentFile = null;
		for(String uploadLocation : uploadLocations){

			if((new File(uploadLocation)).exists()){
				permanentFile = new File(uploadLocation + imageName);
				break;
			}

		}

		if (productImage!=null && !productImage.isEmpty()) {

			try {

				copyFileUsingFileStreams(productImage.getInputStream(), tempFile);

				productImage.transferTo(permanentFile);

			} catch (Exception e) {
				throw new RuntimeException("Product Image saving failed", e);
			}
		}


		model.addAttribute("success", "User " + user.getFirstName() + " "+ user.getLastName() + " registered successfully");
		model.addAttribute("loggedinuser", getPrincipal());

		return "redirect:/list";
	}


	/**
	 * This method will provide the medium to update an existing user.
	 */
	@RequestMapping(value = { "/edit-user-{ssoId}" }, method = RequestMethod.GET)
	public String editUser(@PathVariable String ssoId, ModelMap model) {
		User user = userService.findBySSO(ssoId);
		if(user == null){
			throw new UserNotFoundException("Unable to find user with user name: " + ssoId);
		}
		model.addAttribute("user", user);
		model.addAttribute("edit", true);
		model.addAttribute("loggedinuser", getPrincipal());
		return "user/edit";
	}

	/**
	 * This method will be called on form submission, handling POST request for
	 * updating user in database. It also validates the user input
	 */
	@RequestMapping(value = { "/edit-user-{ssoId}" }, method = RequestMethod.POST)
	public String updateUser(@Valid @ModelAttribute("user") User user, BindingResult result,
							 ModelMap model, @PathVariable String ssoId,
							 HttpServletRequest request) {

		if (result.hasErrors()) {
			if(!result.getFieldErrors().get(0).getField().equals("ssoId")){
				return "user/edit";
			}
		}

		/*//Uncomment below 'if block' if you WANT TO ALLOW UPDATING SSO_ID in UI which is a unique key to a User.
		if(!userService.isUserSSOUnique(user.getId(), user.getSsoId())){
			FieldError ssoError =new FieldError("user","ssoId",messageSource.getMessage("non.unique.ssoId", new String[]{user.getSsoId()}, Locale.getDefault()));
		    result.addError(ssoError);
			return "registration";
		}*/

		userService.updateUser(user);

		MultipartFile productImage = user.getUserProfPic();

		String[] uploadLocations = messageSource
				.getMessage("upload.locations").split(",");

		String[] staticImgPaths = messageSource
				.getMessage("static.img.paths").split(",");

		String imageName = user.getId() + ".png";

		String rootDirectory = request.getSession().getServletContext().getRealPath("/");

		File tempFile = null; //new File(rootDirectory+"static\\img\\"+ imageName);
		for (String staticImgPath : staticImgPaths){
			if(new File(rootDirectory + staticImgPath).exists()){
				tempFile = new File(rootDirectory + staticImgPath + imageName);
				break;
			}
		}

		File permanentFile = null;
		for(String uploadLocation : uploadLocations){

			if((new File(uploadLocation)).exists()){
				permanentFile = new File(uploadLocation + imageName);
				break;
			}

		}

		if (productImage!=null && !productImage.isEmpty()) {

			try {

				copyFileUsingFileStreams(productImage.getInputStream(), tempFile);

				productImage.transferTo(permanentFile);

			} catch (Exception e) {
				throw new RuntimeException("Product Image saving failed", e);
			}
		}



		model.addAttribute("success", "User " + user.getFirstName() + " "+ user.getLastName() + " updated successfully");
		model.addAttribute("loggedinuser", getPrincipal());
		return "redirect:/list";
	}

	/**
	 * This method will delete an user by it's SSOID value.
	 */
	@RequestMapping(value = { "/delete-user-{ssoId}" }, method = RequestMethod.GET)
	public String deleteUser(@PathVariable String ssoId) {
		userService.deleteUserBySSO(ssoId);
		return "redirect:/list";
	}

	/**
	 * This method handles Access-Denied redirect.
	 */
	@RequestMapping(value = "/Access_Denied", method = RequestMethod.GET)
	public String accessDeniedPage(ModelMap model) {
		model.addAttribute("loggedinuser", getPrincipal());
		return "accessDenied";
	}

	/**
	 * This method handles login GET requests.
	 * If users is already logged-in and tries to goto login page again, will be redirected to list page.
	 */
	@RequestMapping(value = "/login", method = RequestMethod.GET)
	public String loginPage() {
		if (userService.isUserAuthenticated()) {
			return "login";
		} else {
			return "redirect:/list";
		}
	}

	/**
	 * This method handles logout requests.
	 * Toggle the handlers if you are RememberMe functionality is useless in your app.
	 */
	@RequestMapping(value="/logout", method = RequestMethod.GET)
	public String logoutPage (HttpServletRequest request, HttpServletResponse response){
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		if (auth != null){
			//new SecurityContextLogoutHandler().logout(request, response, auth);
			//persistentTokenBasedRememberMeServices.logout(request, response, auth);
			SecurityContextHolder.getContext().setAuthentication(null);
		}
		return "redirect:/login?logout";
	}



	/**
	 * This method returns the principal[user-name] of logged-in user.
	 */
	private String getPrincipal(){
		String userName = null;
		Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();

		if (principal instanceof UserDetails) {
			userName = ((UserDetails)principal).getUsername();
		} else {
			userName = principal.toString();
		}
		return userName;
	}



	private static void copyFileUsingFileStreams(InputStream input, File dest)
			throws IOException {
		OutputStream output = null;
		try {
			output = new FileOutputStream(dest);
			byte[] buf = new byte[1024];
			int bytesRead;
			while ((bytesRead = input.read(buf)) > 0) {
				output.write(buf, 0, bytesRead);
			}
		} finally {
			input.close();
			output.close();
		}
	}

}
