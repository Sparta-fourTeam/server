package com.novaserver.user.entity;

import com.novaserver.tutorial.entity.Tutorials;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "user_tutorial",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "tutorial_id"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserTutorial {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tutorial_id")
    private Tutorials tutorials;
}
