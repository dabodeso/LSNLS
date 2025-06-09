package com.lsnls.service;

import com.lsnls.entity.Programa;
import com.lsnls.repository.ProgramaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ProgramaService {

    @Autowired
    private ProgramaRepository programaRepository;

    public List<Programa> findAll() {
        return programaRepository.findAll();
    }

    public Optional<Programa> findById(Long id) {
        return programaRepository.findById(id);
    }

    public Programa create(Programa programa) {
        return programaRepository.save(programa);
    }

    public Programa update(Long id, Programa programa) {
        programa.setId(id);
        return programaRepository.save(programa);
    }

    public void delete(Long id) {
        programaRepository.deleteById(id);
    }
} 