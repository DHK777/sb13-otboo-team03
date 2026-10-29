package com.otboo.team03.feed.service;

import com.otboo.team03.clothes.entity.Clothes;
import com.otboo.team03.clothes.entity.ClothesType;
import com.otboo.team03.clothes.repository.ClothesRepository;
import com.otboo.team03.feed.dto.request.FeedCreateRequest;
import com.otboo.team03.feed.entity.Feed;
import com.otboo.team03.feed.entity.FeedClothes;
import com.otboo.team03.feed.repository.FeedClothesRepository;
import com.otboo.team03.feed.repository.FeedRepository;
import com.otboo.team03.feed.repository.FeedWeatherLookupRepository;
import com.otboo.team03.global.exception.OtbooException;
import com.otboo.team03.user.entity.User;
import com.otboo.team03.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FeedServiceTest {

  @Mock
  private FeedRepository feedRepository;

  @Mock
  private FeedClothesRepository feedClothesRepository;

  @Mock
  private UserRepository userRepository;

  @Mock
  private ClothesRepository clothesRepository;

  @Mock
  private FeedWeatherLookupRepository weatherRepository;

  @InjectMocks
  private FeedService feedService;

  @Test
  void 피드_생성시_의상도_함께_연결된다() {
    // given
    UUID authorId = UUID.randomUUID();
    UUID weatherId = UUID.randomUUID();
    UUID firstClothesId = UUID.randomUUID();
    UUID secondClothesId = UUID.randomUUID();

    User author = mock(User.class);
    when(author.getId()).thenReturn(authorId);

    FeedCreateRequest request = new FeedCreateRequest(
        authorId,
        weatherId,
        List.of(firstClothesId, secondClothesId),
        "오늘의 코디"
    );

    Clothes firstClothes = new Clothes(
        "흰색 티셔츠",
        ClothesType.TOP,
        null,
        author
    );

    Clothes secondClothes = new Clothes(
        "청바지",
        ClothesType.BOTTOM,
        null,
        author
    );

    when(userRepository.existsById(authorId)).thenReturn(true);
    when(weatherRepository.existsById(weatherId)).thenReturn(true);
    when(clothesRepository.findAllById(request.clothesIds()))
        .thenReturn(List.of(firstClothes, secondClothes));
    when(feedRepository.save(any(Feed.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    // when
    Feed result = feedService.create(request);

    // then
    ArgumentCaptor<FeedClothes> captor =
        ArgumentCaptor.forClass(FeedClothes.class);

    verify(feedClothesRepository, times(2)).save(captor.capture());

    List<FeedClothes> links = captor.getAllValues();

    assertAll(
        () -> assertEquals(authorId, result.getAuthorId()),
        () -> assertEquals(weatherId, result.getWeatherId()),
        () -> assertEquals("오늘의 코디", result.getContent()),
        () -> assertEquals(2, links.size()),
        () -> assertTrue(links.stream().allMatch(
            link -> link.getFeed() == result
        )),
        () -> assertTrue(links.stream().anyMatch(
            link -> link.getClothes() == firstClothes
        )),
        () -> assertTrue(links.stream().anyMatch(
            link -> link.getClothes() == secondClothes
        ))
    );
  }

  @Test
  void 작성자가_존재하지_않으면_피드를_생성할_수_없다() {
    // given
    UUID authorId = UUID.randomUUID();

    FeedCreateRequest request = new FeedCreateRequest(
        authorId,
        UUID.randomUUID(),
        List.of(),
        "오늘의 코디"
    );

    when(userRepository.existsById(authorId)).thenReturn(false);

    // when
    OtbooException exception = assertThrows(
        OtbooException.class,
        () -> feedService.create(request)
    );

    // then
    assertNotNull(exception);
    verify(feedRepository, never()).save(any(Feed.class));
  }

  @Test
  void 날씨가_존재하지_않으면_피드를_생성할_수_없다() {
    // given
    UUID authorId = UUID.randomUUID();
    UUID weatherId = UUID.randomUUID();

    FeedCreateRequest request = new FeedCreateRequest(
        authorId,
        weatherId,
        List.of(),
        "오늘의 코디"
    );

    when(userRepository.existsById(authorId)).thenReturn(true);
    when(weatherRepository.existsById(weatherId)).thenReturn(false);

    // when
    OtbooException exception = assertThrows(
        OtbooException.class,
        () -> feedService.create(request)
    );

    // then
    assertNotNull(exception);
    verify(feedRepository, never()).save(any(Feed.class));
  }

  @Test
  void 존재하지_않는_의상이_포함되면_피드를_생성할_수_없다() {
    // given
    UUID authorId = UUID.randomUUID();
    UUID weatherId = UUID.randomUUID();
    UUID clothesId = UUID.randomUUID();

    FeedCreateRequest request = new FeedCreateRequest(
        authorId,
        weatherId,
        List.of(clothesId),
        "오늘의 코디"
    );

    when(userRepository.existsById(authorId)).thenReturn(true);
    when(weatherRepository.existsById(weatherId)).thenReturn(true);
    when(clothesRepository.findAllById(request.clothesIds()))
        .thenReturn(List.of());

    // when
    OtbooException exception = assertThrows(
        OtbooException.class,
        () -> feedService.create(request)
    );

    // then
    assertNotNull(exception);
    verify(feedRepository, never()).save(any(Feed.class));
  }

  @Test
  void 중복된_의상_ID가_있으면_피드를_생성할_수_없다() {
    // given
    UUID clothesId = UUID.randomUUID();

    FeedCreateRequest request = new FeedCreateRequest(
        UUID.randomUUID(),
        UUID.randomUUID(),
        List.of(clothesId, clothesId),
        "오늘의 코디"
    );

    // when
    OtbooException exception = assertThrows(
        OtbooException.class,
        () -> feedService.create(request)
    );

    // then
    assertNotNull(exception);
    verify(feedRepository, never()).save(any(Feed.class));
  }

  @Test
  void 다른_사용자의_의상으로_피드를_생성할_수_없다() {
    // given
    UUID authorId = UUID.randomUUID();
    UUID weatherId = UUID.randomUUID();
    UUID clothesId = UUID.randomUUID();
    UUID otherUserId = UUID.randomUUID();

    User otherUser = mock(User.class);
    when(otherUser.getId()).thenReturn(otherUserId);

    FeedCreateRequest request = new FeedCreateRequest(
        authorId,
        weatherId,
        List.of(clothesId),
        "오늘의 코디"
    );

    Clothes otherUserClothes = new Clothes(
        "다른 사용자의 티셔츠",
        ClothesType.TOP,
        null,
        otherUser
    );

    when(userRepository.existsById(authorId)).thenReturn(true);
    when(weatherRepository.existsById(weatherId)).thenReturn(true);
    when(clothesRepository.findAllById(request.clothesIds()))
        .thenReturn(List.of(otherUserClothes));

    // when
    OtbooException exception = assertThrows(
        OtbooException.class,
        () -> feedService.create(request)
    );

    // then
    assertNotNull(exception);
    verify(feedRepository, never()).save(any(Feed.class));
    verify(feedClothesRepository, never()).save(any(FeedClothes.class));
  }
}
