package com.mballem.curso.security.web.controller;

import java.util.Arrays;
import java.util.List;

import javax.mail.MessagingException;
import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.mballem.curso.security.domain.Medico;
import com.mballem.curso.security.domain.Perfil;
import com.mballem.curso.security.domain.PerfilTipo;
import com.mballem.curso.security.domain.Usuario;
import com.mballem.curso.security.service.MedicoService;
import com.mballem.curso.security.service.UsuarioService;

//@Controller
@RequestMapping("u")
public class UsuarioController {

	@Autowired
	private UsuarioService service;
	@Autowired
	private MedicoService medicoService;
	
	//abrir cadastro de usuários (medico, admin e paciente)
	@GetMapping("/novo/cadastro/usuario")
	public String cadastroPorAdminParaAdminMedicoPaciente(Usuario usuario) {
		
		return "usuario/cadastro";
	}
	
	//abrir pagina da lista de usuarios
	@GetMapping("/lista")
	public String listarUsuarios() {
		
		return "usuario/lista";
	}
	
	//obter os usuarios da tabela para compor o DataTables
	@GetMapping("/datatables/server/usuarios")
	public ResponseEntity<?> obtemUsuariosDatatable(HttpServletRequest request) {
		
		return ResponseEntity.ok(service.buscarUsuarios(request));
	}
	
	@PostMapping("/cadastro/salvar")
	public String listarUsuarios(Usuario usuario, RedirectAttributes attr) {
		List<Perfil> perfis = usuario.getPerfis();
		if (perfis.size() > 2 ||
				perfis.containsAll(Arrays.asList(new Perfil(1L), new Perfil(3L))) ||
				perfis.containsAll(Arrays.asList(new Perfil(2L), new Perfil(3L)))) {
			attr.addFlashAttribute("falha", "Paciente não pode ser ADMIN e/ou MÉDICO!");
			attr.addFlashAttribute("usuario", usuario);
		} else {
			try {
				service.salvarUsuario(usuario);
				attr.addFlashAttribute("sucesso", "Operação realizada com sucesso!");
			} catch (DataIntegrityViolationException exception) {
				attr.addFlashAttribute("falha", "Cadastro não realizado, email já existe!");
			}
		}
		return "redirect:/u/novo/cadastro/usuario";
	}
	
	@GetMapping("/editar/credenciais/usuario/{id}")
	public ModelAndView preEditarCredenciais(@PathVariable("id") Long id) {
		
		return new ModelAndView("usuario/cadastro", "usuario", service.buscarPorId(id));
	}
	
	@GetMapping("/editar/dados/usuario/{id}/perfis/{perfis}")
	public ModelAndView preEditarDadosPessoais(@PathVariable("id") Long usuarioId, @PathVariable("perfis") Long[] perfisId) {
		
		Usuario user = service.buscarPorIdEPerfis(usuarioId, perfisId);
			
		if (user.getPerfis().contains(new Perfil(PerfilTipo.ADMIN.getCod())) &&
				!user.getPerfis().contains(new Perfil(PerfilTipo.MEDICO.getCod()))) {
			return new ModelAndView("usuario/cadastro", "usuario", user);
		} else if (user.getPerfis().contains(new Perfil(PerfilTipo.MEDICO.getCod()))) {
			
			Medico medico = medicoService.buscarPorUsuarioId(usuarioId);
			return medico.hasNotId() 
					? new ModelAndView("medico/cadastro", "medico", new Medico(new Usuario(usuarioId))) 
					: new ModelAndView("medico/cadastro", "medico", medico);
		} else if (user.getPerfis().contains(new Perfil(PerfilTipo.PACIENTE.getCod()))) {
			ModelAndView model = new ModelAndView("error");
			model.addObject("status", 403);
			model.addObject("error", "Área Rstrita");
			model.addObject("message", "Os dados de paciente são restritos a ele");
			
			return model;
		}
		return new ModelAndView("redirect:/u/lista");
	}
	
	@GetMapping("/editar/senha")
	public String abrirEditarSenha() {
		return "usuario/editar-senha";
	}
	
	@PostMapping("/confirmar/senha")
	public String editarSenha(@RequestParam("senha1") String s1, 
								@RequestParam("senha2") String s2, 
								@RequestParam("senha3") String s3, 
								@AuthenticationPrincipal User user, RedirectAttributes attr) {
		if (!s1.equals(s2)) {
			attr.addFlashAttribute("falha", "Senhas não coincidem. Tente novamente.");
			return "redirect:/u/editar/senha";
		}
		Usuario u = service.buscarPorEmail(user.getUsername());
		if (!UsuarioService.isSenhaCorreta(s3, u.getSenha())) {
			attr.addFlashAttribute("falha", "Senha atual não confere. Tente novamente.");
			return "redirect:/u/editar/senha";
		}
		service.alterarSenha(u, s1);
		attr.addFlashAttribute("sucesso", "Senha alterada com sucesso.");
		return "redirect:/u/editar/senha";
	}
	
	@GetMapping("/novo/cadastro")
	public String novoCadastro(Usuario usuario) {
		
		return "cadastrar-se";
	}
	
	@GetMapping("/cadastro/realizado")
	public String cadastroRealizado() {
		
		return "fragments/mensagem";
	}
	
	@PostMapping("/cadastro/paciente/salvar")
	public String salvar(Usuario usuario, BindingResult result) throws MessagingException {
		try {
			service.salvarCadastroPaciente(usuario);
		} catch (DataIntegrityViolationException e) {
			result.reject("email", "Ops...esse email já está cadastrado.");
			return "cadastrar-se";
		}
		return "redirect:/u/cadastro/realizado";
	}
	
	@GetMapping("/confirmacao/cadastro")
	public String respostaConfirmacaoCadastroPaciente(@RequestParam("codigo") String codigo, RedirectAttributes attr) {
		service.ativarCadastroPaciente(codigo);
		
		attr.addFlashAttribute("alerta", "sucesso");
		attr.addFlashAttribute("titulo", "Cadastro Ativado!");
		attr.addFlashAttribute("texto", "Parabéns, seu cadastro está ativo.");
		attr.addFlashAttribute("subtexto", "Pode logar.");
		
		return "redirect:/login";
	}
	
	@GetMapping("p/redefinir/senha")
	public String pedidoRedefinirSenha() {
		
		return "usuario/pedido-recuperar-senha";
	}
	
	@GetMapping("p/recuperar/senha")
	public String redefinirSenha(String email, ModelMap model) throws MessagingException {
		service.pedidoRedefinicaoSenha(email);
		model.addAttribute("sucesso", "Em instantes você receberá um email para redefinir sua senha");
				
		return "usuario/recuperar-senha";
	}
	
	@PostMapping("p/nova/senha")
	public String confirmacaoRedefinirSenha(Usuario usuario, ModelMap model) throws MessagingException {
		Usuario u = service.buscarPorEmail(usuario.getEmail());
		if (!u.getCodigoVerificador().equals(usuario.getCodigoVerificador())) {
			model.addAttribute("falha", "Código verificador não confere!");
			return "usuario/recuperar-senha";
		}
		u.setCodigoVerificador(null);
		service.alterarSenha(u, usuario.getSenha());
		model.addAttribute("alerta", "sucesso");
		model.addAttribute("titulo", "Senha redefinida!");
		model.addAttribute("texto", "Já pode logar no sistema.");
				
		return "login";
	}
	
}
