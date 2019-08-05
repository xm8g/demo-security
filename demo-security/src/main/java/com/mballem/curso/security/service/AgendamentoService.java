package com.mballem.curso.security.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mballem.curso.security.datatables.Datatables;
import com.mballem.curso.security.datatables.DatatablesColunas;
import com.mballem.curso.security.domain.Agendamento;
import com.mballem.curso.security.domain.Horario;
import com.mballem.curso.security.repository.AgendamentoRepository;
import com.mballem.curso.security.repository.projection.HistoricoPaciente;
import com.mballem.curso.security.web.exception.AcessoNegadoException;

@Service
public class AgendamentoService {

	@Autowired
	private AgendamentoRepository agendamentoRepository;

	@Autowired
	private Datatables datatables;

	@Transactional(readOnly = true)
	public List<Horario> buscarHorariosDisponiveisDoMedico(Long id, LocalDate data) {

		return agendamentoRepository.findMedicoDisponivelNaData(id, data);
	}

	@Transactional(readOnly = false)
	public void salvar(Agendamento agendamento) {
		agendamentoRepository.save(agendamento);
	}

	@Transactional(readOnly = true)
	public Map<String, Object> buscarHistoricoPorPaciente(String email, HttpServletRequest req) {
		datatables.setRequest(req);
		datatables.setColunas(DatatablesColunas.AGENDAMENTOS);
		Page<HistoricoPaciente> page = agendamentoRepository.findHistoricoByPacienteEmail(email,
				datatables.getPageable());
		return datatables.getResponse(page);
	}

	@Transactional(readOnly = true)
	public Map<String, Object> buscarHistoricoPorMedico(String email, HttpServletRequest req) {
		datatables.setRequest(req);
		datatables.setColunas(DatatablesColunas.AGENDAMENTOS);
		Page<HistoricoPaciente> page = agendamentoRepository.findHistoricoByMedicoEmail(email,
				datatables.getPageable());
		return datatables.getResponse(page);
	}

	@Transactional(readOnly = true)
	public Agendamento buscarPorId(Long id) {
		return agendamentoRepository.findById(id).get();
	}

	@Transactional(readOnly = false)
	public void editar(Agendamento agendamento, String username) {
		Agendamento ag = buscarPorIdEUsuario(agendamento.getId(), username);
		ag.setEspecialidade(agendamento.getEspecialidade());
		ag.setDataConsulta(agendamento.getDataConsulta());
		ag.setHorario(agendamento.getHorario());
		ag.setMedico(agendamento.getMedico());
	}

	@Transactional(readOnly = true)
	public Agendamento buscarPorIdEUsuario(Long id, String username) {
		return agendamentoRepository.findByIdAndPacienteOrMedicoEmail(id, username)
				.orElseThrow(() -> new AcessoNegadoException("Acesso negado ao usuário: " + username));
	}

	@Transactional(readOnly = false)
	public void remover(Long id) {
		agendamentoRepository.deleteById(id);
	}

}
