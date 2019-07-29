package com.mballem.curso.security.web.controller;

import java.util.Arrays;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.mballem.curso.security.domain.Perfil;
import com.mballem.curso.security.domain.Usuario;
import com.mballem.curso.security.service.UsuarioService;

@Controller
@RequestMapping("u")
public class UsuarioController {

	@Autowired
	private UsuarioService service;
	
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
				perfis.contains(Arrays.asList(new Perfil(1L), new Perfil(3L))) ||
				perfis.contains(Arrays.asList(new Perfil(2L), new Perfil(3L)))) {
			attr.addFlashAttribute("falha", "Paciente não pode ser ADMIN e/ou MÉDICO!");
			attr.addFlashAttribute("usuario", usuario);
		} else {
			service.salvarUsuario(usuario);
			attr.addFlashAttribute("sucesso", "Operação realizada com sucesso!");
		}
		return "redirect:/u/novo/cadastro/usuario";
	}
}
