package com.eventpro.service;

import com.eventpro.exception.notfound.ResourceNotFoundException;
import com.eventpro.entity.users.Client;
import com.eventpro.entity.users.Organizer;
import com.eventpro.entity.users.User;
import com.eventpro.repository.ClientRepository;
import com.eventpro.repository.OrganizerRepository;
import com.eventpro.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final ClientRepository clientRepository;
    private final OrganizerRepository organizerRepository;

    @Transactional(readOnly = true)
    public Page<User> findAll(Pageable pageable) {
        return userRepository.findAll(pageable);
    }

    @Transactional(readOnly = true)
    public Client getClient(Long id) {
        return clientRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Client " + id + " not found"));
    }

    @Transactional(readOnly = true)
    public Organizer getOrganizer(Long id) {
        return organizerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Organizer " + id + " not found"));
    }
}
