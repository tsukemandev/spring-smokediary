package io.github.tsukemendog.nondesire.controller;

import io.github.tsukemendog.nondesire.entity.Daily;
import io.github.tsukemendog.nondesire.entity.Member;
import io.github.tsukemendog.nondesire.service.DailyService;
import io.github.tsukemendog.nondesire.service.MemberService;
import lombok.AllArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;

@Controller
@RequestMapping("/diary")
@AllArgsConstructor
public class DailyController {

    private final DailyService dailyService;
    private final MemberService memberService;
    @GetMapping
    public String daily(Model model, @AuthenticationPrincipal OAuth2User oAuth2User, OAuth2AuthenticationToken token) {
        if (oAuth2User == null) {
            model.addAttribute("totalDays", dailyService.getTotalDays());
            return "daily";
        }
        Member member = memberService.getMemberByOAuth2UserAndToken(oAuth2User, token);
        model.addAttribute("totalDays", dailyService.getCheckedTotalDays(member));
        return "daily";
    }

    @GetMapping("/re/{id}")
    public String daily01(Model model, @AuthenticationPrincipal OAuth2User oAuth2User, OAuth2AuthenticationToken token, @PathVariable String id) {
        model.addAttribute("totalDays", dailyService.getTotalDays());
        if (!id.matches("\\d+")) {
            return "daily";
        }

        Member member = memberService.getMemberByOAuth2UserAndToken(oAuth2User, token);
        Optional<Daily> dailyOptional = dailyService.findByIdAndMember(Long.parseLong(id), member);

        if (dailyOptional.isEmpty()) {
            return "daily";
        }

        Daily daily = dailyOptional.get();
        model.addAttribute("title", daily.getTitle());
        model.addAttribute("content", daily.getContent());

        return "daily01";
    }

    @GetMapping("/post")
    public String showPost(Model model, @AuthenticationPrincipal OAuth2User oAuth2User, OAuth2AuthenticationToken token) {
        return "post";
    }

    @PostMapping("/reset")
    public String reset(@AuthenticationPrincipal OAuth2User oAuth2User, OAuth2AuthenticationToken token) {
        dailyService.resetDiary(oAuth2User, token);

        return "redirect:/";
    }

    @PostMapping("/post")
    public String processPost(@AuthenticationPrincipal OAuth2User oAuth2User,
                              OAuth2AuthenticationToken token,
                              Model model,
                              @RequestParam Map<String, String> request) {
        Member member = memberService.getMemberByOAuth2UserAndToken(oAuth2User, token);
        Daily daily = dailyService.getRecentDaily(member);
        if (daily == null) {
            memberService.start(member);
            dailyService.save(member, Daily.builder()
                    .days(0)
                    .build(), request);
            return "redirect:/";
        }

        if (daily.getDays() >= DailyService.limitDay) {
            model.addAttribute("message","You have already completed the mission. If you wish to try again, please click the reset button on the date screen!");
            return "post";
        }

        if (!dailyService.checkDuplicateDiary(daily.getRegDate())) {
            model.addAttribute("message","You can only write a diary once a day.");
            model.addAttribute("totalDays", dailyService.getCheckedTotalDays(member));
            return "daily";
        }

        dailyService.save(member, daily, request);
        return "redirect:/diary";
    }
}
